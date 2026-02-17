package io.tiagovportela.inference.posetracker;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.FramePreprocessor;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.tensorflowlite.BuiltinOpResolver;
import org.bytedeco.tensorflowlite.FlatBufferModel;
import org.bytedeco.tensorflowlite.Interpreter;
import org.bytedeco.tensorflowlite.InterpreterBuilder;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

/**
 * Estimates 33 body-pose landmarks using the MediaPipe BlazePose
 * {@code pose_landmark_heavy.tflite} model via TensorFlow Lite (bytedeco).
 *
 * <p>
 * Receives the original frame and a {@link BoundingBox} from the detection
 * stage, crops and preprocesses the region internally, runs inference, and
 * maps the resulting landmarks back to original-frame coordinates.
 * </p>
 */
public class PoseTracker {

    /* ---- model constants ---- */
    private static final int INPUT_SIZE = 256;
    private static final int NUM_LANDMARKS = 33;
    private static final int VALUES_PER_LANDMARK = 5; // x, y, z, visibility, presence
    private static final int TOTAL_LANDMARK_VALUES = 195; // 39 × 5 (33 pose + 6 auxiliary)
    private static final float MIN_POSE_PRESENCE = 0.5f;

    private final Interpreter interpreter;
    private final FramePreprocessor preprocessor;

    /**
     * Creates a PoseTracker loading the TFLite model from the given path.
     *
     * @param modelPath absolute or relative path to
     *                  {@code pose_landmark_heavy.tflite}
     */
    public PoseTracker(String modelPath) {
        FlatBufferModel model = FlatBufferModel.BuildFromFile(modelPath);
        if (model == null || model.isNull()) {
            throw new RuntimeException("Failed to load TFLite model: " + modelPath);
        }

        BuiltinOpResolver resolver = new BuiltinOpResolver();
        InterpreterBuilder builder = new InterpreterBuilder(model, resolver);

        this.interpreter = new Interpreter((Interpreter) null);
        if (builder.apply(interpreter) != 0) {
            throw new RuntimeException("Failed to build TFLite interpreter");
        }
        if (interpreter.AllocateTensors() != 0) {
            throw new RuntimeException("Failed to allocate TFLite tensors");
        }

        this.preprocessor = new FramePreprocessor(INPUT_SIZE);
    }

    /**
     * Estimates pose landmarks from the original frame using the given bounding
     * box.
     *
     * <p>
     * The bounding box region is cropped from the frame, resized to 256×256,
     * normalised, and fed through the landmark model. Returned landmarks are
     * mapped back to original-frame normalised coordinates.
     * </p>
     *
     * @param frame       the original BGR image (any size)
     * @param boundingBox the detection-stage bounding box identifying the person
     * @return array of 33 {@link Landmark}s in original-frame coordinates,
     *         or {@code null} if pose presence is below the confidence threshold
     */
    public Landmark[] track(Mat frame, BoundingBox boundingBox) {
        // 1. Crop to bounding box ────────────────────────────────────
        int[] abs = boundingBox.toAbsolute(frame.cols(), frame.rows());
        int x = Math.max(0, abs[0]);
        int y = Math.max(0, abs[1]);
        int w = Math.min(abs[2], frame.cols() - x);
        int h = Math.min(abs[3], frame.rows() - y);

        Mat cropped = new Mat(frame, new Rect(x, y, w, h));
        float[] inputData = preprocessor.preprocess(cropped);
        cropped.release();

        // 2. Write to the interpreter's input tensor ─────────────────
        int totalPixels = INPUT_SIZE * INPUT_SIZE * 3;
        FloatPointer inputPtr = interpreter.typed_input_tensor_float(0);
        inputPtr.put(inputData, 0, totalPixels);

        // 3. Invoke ──────────────────────────────────────────────────
        if (interpreter.Invoke() != 0) {
            throw new RuntimeException("TFLite inference failed");
        }

        // 4. Identify outputs ────────────────────────────────────────
        // The model outputs at index 0 and 1; identify by flattened size:
        // - landmarks [1, 195] (39 × 5)
        // - pose flag [1, 1]
        long out0Size = 1;
        for (int d = 0; d < interpreter.output_tensor(0).dims().size(); d++) {
            out0Size *= interpreter.output_tensor(0).dims().data(d);
        }

        int landmarkOutputIdx;
        int posePresenceIdx;

        if (out0Size == TOTAL_LANDMARK_VALUES) {
            landmarkOutputIdx = 0;
            posePresenceIdx = 1;
        } else {
            landmarkOutputIdx = 1;
            posePresenceIdx = 0;
        }

        // 5. Check pose presence ─────────────────────────────────────
        float[] presenceData = new float[1];
        interpreter.typed_output_tensor_float(posePresenceIdx).get(presenceData, 0, 1);
        float posePresence = sigmoid(presenceData[0]);
        if (posePresence < MIN_POSE_PRESENCE) {
            return null;
        }

        // 6. Read landmark data ──────────────────────────────────────
        float[] rawLandmarks = new float[TOTAL_LANDMARK_VALUES];
        interpreter.typed_output_tensor_float(landmarkOutputIdx).get(rawLandmarks, 0, TOTAL_LANDMARK_VALUES);

        // 7. Decode and remap to original-frame coordinates ──────────
        Landmark[] landmarks = new Landmark[NUM_LANDMARKS];

        float boxX = boundingBox.getXMin();
        float boxY = boundingBox.getYMin();
        float boxW = boundingBox.getWidth();
        float boxH = boundingBox.getHeight();

        for (int i = 0; i < NUM_LANDMARKS; i++) {
            int off = i * VALUES_PER_LANDMARK;

            // Landmark x, y are normalised to the 256×256 crop (0–1)
            float cropX = rawLandmarks[off] / INPUT_SIZE;
            float cropY = rawLandmarks[off + 1] / INPUT_SIZE;
            float z = rawLandmarks[off + 2];
            float visibility = sigmoid(rawLandmarks[off + 3]);
            float presence = sigmoid(rawLandmarks[off + 4]);

            // Map back to original-frame normalised coordinates
            float frameX = boxX + cropX * boxW;
            float frameY = boxY + cropY * boxH;

            landmarks[i] = new Landmark(frameX, frameY, z, visibility, presence);
        }

        return landmarks;
    }

    /*
     * ================================================================
     * Private helpers
     * ================================================================
     */

    private static float sigmoid(float x) {
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }
}
