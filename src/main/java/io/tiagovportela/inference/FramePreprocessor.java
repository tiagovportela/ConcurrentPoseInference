package io.tiagovportela.inference;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Handles image preprocessing for the pose detection model.
 *
 * <p>
 * Steps performed:
 * </p>
 * <ol>
 * <li>Resize to the model's expected input dimensions</li>
 * <li>Convert BGR → RGB</li>
 * <li>Normalise pixel values to [0, 1]</li>
 * <li>Extract a flat {@code float[]} suitable for the TFLite input tensor</li>
 * </ol>
 */
public class FramePreprocessor {

    private final int inputSize;

    /**
     * Creates a preprocessor targeting the given square input size.
     *
     * @param inputSize width and height expected by the model (e.g. 224)
     */
    public FramePreprocessor(int inputSize) {
        this.inputSize = inputSize;
    }

    /**
     * Preprocesses a BGR frame into a normalised float array ready for inference.
     *
     * @param frame BGR image of any size
     * @return float array of shape [inputSize * inputSize * 3] with values in [0,
     *         1]
     */
    public float[] preprocess(Mat frame) {
        Mat resized = new Mat();
        Imgproc.resize(frame, resized, new Size(inputSize, inputSize));
        Imgproc.cvtColor(resized, resized, Imgproc.COLOR_BGR2RGB);
        resized.convertTo(resized, CvType.CV_32FC3, 1.0 / 255.0);

        int totalPixels = inputSize * inputSize * 3;
        float[] inputData = new float[totalPixels];
        resized.get(0, 0, inputData);
        resized.release();

        return inputData;
    }
}
