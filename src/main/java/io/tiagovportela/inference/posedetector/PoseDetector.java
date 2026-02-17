package io.tiagovportela.inference.posedetector;

import io.tiagovportela.datatypes.BoundingBox;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.tensorflowlite.BuiltinOpResolver;
import org.bytedeco.tensorflowlite.FlatBufferModel;
import org.bytedeco.tensorflowlite.Interpreter;
import org.bytedeco.tensorflowlite.InterpreterBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects a person bounding box using the MediaPipe BlazePose
 * {@code pose_detection.tflite} model via TensorFlow Lite (bytedeco).
 *
 * <p>
 * The model is an SSD-based detector with 2 254 anchor-based predictions.
 * This class handles anchor generation, detection decoding, score filtering,
 * and weighted non-max suppression.
 * </p>
 */
public class PoseDetector {

    /* ---- model constants ---- */
    private static final int INPUT_SIZE = 224;
    private static final int[] STRIDES = { 8, 16, 32, 32, 32 };
    private static final int ANCHORS_PER_CELL = 2;
    private static final double ANCHOR_OFFSET = 0.5;
    private static final int NUM_BOXES = 2254;
    private static final int NUM_VALUES = 12; // cx, cy, w, h + 4 keypoint pairs
    private static final float MIN_SCORE_THRESH = 0.5f;
    private static final float NMS_IOU_THRESH = 0.3f;
    private static final float ROI_SCALE_FACTOR = 1.25f;

    private final Interpreter interpreter;
    private final float[][] anchors; // [NUM_BOXES][2] (center_x, center_y)

    /**
     * Creates a PoseDetector loading the TFLite model from the given path.
     *
     * @param modelPath absolute or relative path to {@code pose_detection.tflite}
     */
    public PoseDetector(String modelPath) {
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

        this.anchors = generateAnchors();
    }

    /**
     * Detects the most prominent person bounding box from preprocessed input data.
     *
     * @param inputData preprocessed float array of shape [224*224*3] with values in
     *                  [0, 1]
     * @return the best {@link BoundingBox} (normalised 0–1), or {@code null}
     *         if no person is detected above the confidence threshold
     */
    public BoundingBox detect(float[] inputData) {
        // 1. Write to the interpreter's input tensor ─────────────────
        int totalPixels = INPUT_SIZE * INPUT_SIZE * 3;
        FloatPointer inputPtr = interpreter.typed_input_tensor_float(0);
        inputPtr.put(inputData, 0, totalPixels);

        // 2. Invoke ──────────────────────────────────────────────────
        if (interpreter.Invoke() != 0) {
            throw new RuntimeException("TFLite inference failed");
        }

        // 3. Read outputs ────────────────────────────────────────────
        // Output 0: regressors [1, 2254, 12] Output 1: scores [1, 2254, 1]
        FloatPointer out0Ptr = interpreter.typed_output_tensor_float(0);
        FloatPointer out1Ptr = interpreter.typed_output_tensor_float(1);

        float[] regressorData = new float[NUM_BOXES * NUM_VALUES];
        float[] scoreData = new float[NUM_BOXES];

        // Determine which output is which based on size
        long out0Size = 1;
        for (int d = 0; d < interpreter.output_tensor(0).dims().size(); d++) {
            out0Size *= interpreter.output_tensor(0).dims().data(d);
        }

        if (out0Size == NUM_BOXES * NUM_VALUES) {
            out0Ptr.get(regressorData, 0, regressorData.length);
            out1Ptr.get(scoreData, 0, scoreData.length);
        } else {
            // Outputs are swapped
            out1Ptr.get(regressorData, 0, regressorData.length);
            out0Ptr.get(scoreData, 0, scoreData.length);
        }

        // 4. Decode detections ───────────────────────────────────────
        // Each detection has: [cx, cy, w, h, kp0_x, kp0_y, kp1_x, kp1_y, kp2_x, kp2_y,
        // kp3_x, kp3_y]
        // kp0 = mid-hip center, kp1 = full-body top (above head)
        List<float[]> boxes = new ArrayList<>(); // [xMin, yMin, w, h]
        List<float[]> keypoints = new ArrayList<>(); // [kp0_x, kp0_y, kp1_x, kp1_y]
        List<Float> scores = new ArrayList<>();

        for (int i = 0; i < NUM_BOXES; i++) {
            float score = sigmoid(scoreData[i]);
            if (score < MIN_SCORE_THRESH)
                continue;

            int off = i * NUM_VALUES;
            float cx = regressorData[off] / INPUT_SIZE + anchors[i][0];
            float cy = regressorData[off + 1] / INPUT_SIZE + anchors[i][1];
            float w = regressorData[off + 2] / INPUT_SIZE;
            float h = regressorData[off + 3] / INPUT_SIZE;

            // Extract keypoints (decoded same as cx/cy: offset/INPUT_SIZE + anchor)
            float kp0x = regressorData[off + 4] / INPUT_SIZE + anchors[i][0];
            float kp0y = regressorData[off + 5] / INPUT_SIZE + anchors[i][1];
            float kp1x = regressorData[off + 6] / INPUT_SIZE + anchors[i][0];
            float kp1y = regressorData[off + 7] / INPUT_SIZE + anchors[i][1];

            float xMin = cx - w / 2f;
            float yMin = cy - h / 2f;

            // Clamp to [0, 1]
            xMin = Math.max(0f, Math.min(1f, xMin));
            yMin = Math.max(0f, Math.min(1f, yMin));
            w = Math.max(0f, Math.min(1f - xMin, w));
            h = Math.max(0f, Math.min(1f - yMin, h));

            boxes.add(new float[] { xMin, yMin, w, h });
            keypoints.add(new float[] { kp0x, kp0y, kp1x, kp1y });
            scores.add(score);
        }

        if (boxes.isEmpty())
            return null;

        // 5. Weighted NMS ────────────────────────────────────────────
        int bestIdx = weightedNMS(boxes, scores);
        if (bestIdx < 0)
            return null;

        // 6. Compute full-body ROI from keypoints ────────────────────
        return computeFullBodyROI(keypoints.get(bestIdx), scores.get(bestIdx));
    }

    /**
     * Computes the full-body ROI from the detection keypoints.
     * <p>
     * Uses MediaPipe convention: keypoint 0 = mid-hip center,
     * keypoint 1 = full-body top (above head). The ROI is a square
     * centred on the person, scaled by 1.25× the distance between
     * these two keypoints.
     * </p>
     */
    private static BoundingBox computeFullBodyROI(float[] kps, float score) {
        float hipX = kps[0], hipY = kps[1];
        float topX = kps[2], topY = kps[3];

        // Center of the ROI: midpoint between hip and top
        float centerX = (hipX + topX) / 2f;
        float centerY = (hipY + topY) / 2f;

        // Scale: distance between keypoints × 1.25 padding factor
        float dx = topX - hipX;
        float dy = topY - hipY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float roiSize = dist * ROI_SCALE_FACTOR;

        // Build square ROI
        float xMin = centerX - roiSize / 2f;
        float yMin = centerY - roiSize / 2f;

        // Clamp to [0, 1]
        xMin = Math.max(0f, xMin);
        yMin = Math.max(0f, yMin);
        float w = Math.min(roiSize, 1f - xMin);
        float h = Math.min(roiSize, 1f - yMin);

        return new BoundingBox(xMin, yMin, w, h, score);
    }

    /*
     * ================================================================
     * Private helpers
     * ================================================================
     */

    /**
     * Generates the 2 254 SSD anchors used by the BlazePose detector.
     * Configuration matches the MediaPipe SsdAnchorsCalculator:
     * <ul>
     * <li>5 layers, strides [8, 16, 32, 32, 32]</li>
     * <li>2 anchors per grid cell</li>
     * <li>anchor_offset 0.5, fixed_anchor_size true</li>
     * </ul>
     */
    private static float[][] generateAnchors() {
        List<float[]> anchorList = new ArrayList<>();

        for (int stride : STRIDES) {
            int gridSize = INPUT_SIZE / stride;
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    float cx = (x + (float) ANCHOR_OFFSET) / gridSize;
                    float cy = (y + (float) ANCHOR_OFFSET) / gridSize;
                    for (int a = 0; a < ANCHORS_PER_CELL; a++) {
                        anchorList.add(new float[] { cx, cy });
                    }
                }
            }
        }

        return anchorList.toArray(new float[0][]);
    }

    /**
     * Simple weighted NMS: picks the highest-scoring detection while
     * suppressing overlapping boxes.
     *
     * @return index of the best surviving detection, or -1
     */
    private static int weightedNMS(List<float[]> boxes, List<Float> scores) {
        int n = boxes.size();
        boolean[] suppressed = new boolean[n];
        int bestIdx = -1;
        float bestScore = -1f;

        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++)
            indices[i] = i;
        java.util.Arrays.sort(indices, (a, b) -> Float.compare(scores.get(b), scores.get(a)));

        for (int i = 0; i < n; i++) {
            int idx = indices[i];
            if (suppressed[idx])
                continue;

            if (scores.get(idx) > bestScore) {
                bestScore = scores.get(idx);
                bestIdx = idx;
            }

            for (int j = i + 1; j < n; j++) {
                int other = indices[j];
                if (suppressed[other])
                    continue;
                if (iou(boxes.get(idx), boxes.get(other)) > NMS_IOU_THRESH) {
                    suppressed[other] = true;
                }
            }
        }

        return bestIdx;
    }

    /** Computes Intersection-over-Union for two [xMin, yMin, w, h] boxes. */
    private static float iou(float[] a, float[] b) {
        float x1 = Math.max(a[0], b[0]);
        float y1 = Math.max(a[1], b[1]);
        float x2 = Math.min(a[0] + a[2], b[0] + b[2]);
        float y2 = Math.min(a[1] + a[3], b[1] + b[3]);

        float intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float union = a[2] * a[3] + b[2] * b[3] - intersection;
        return union > 0 ? intersection / union : 0;
    }

    private static float sigmoid(float x) {
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }
}
