package io.tiagovportela;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.metrics.FpsCsvExporter;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.videoproducer.CameraSource;
import nu.pattern.OpenCV;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Load OpenCV native library (required before any OpenCV API call)
        OpenCV.loadLocally();

        // Resolve the frames directory relative to the source tree
        File framesDir = new File("src/main/java/io/tiagovportela/videoproducer/frames");
        PoseDetector detector = new PoseDetector("src/main/resources/models/pose_detection.tflite");
        FramePreprocessor preprocessor = new FramePreprocessor(224);

        // Pluggable metrics listener — swap implementation to change where data goes
        FrameMetricsListener metricsListener = new FpsCsvExporter("fps_metrics.csv");

        try {
            CameraSource camera = new CameraSource(framesDir);
            System.out.println("Loaded " + camera.getTotalFrames() + " frames.");

            metricsListener.onStart();

            // Push-based playback: iterate every frame via callback
            camera.startPlayback((frame, index) -> {
                long t0 = System.nanoTime();

                System.out.printf("Frame %04d: %dx%d%n",
                        index, frame.cols(), frame.rows());
                float[] input = preprocessor.preprocess(frame);
                BoundingBox box = detector.detect(input);
                if (box != null) {
                    System.out.println(box);
                }

                metricsListener.onFrameProcessed(index, System.nanoTime() - t0);
            });

            metricsListener.onFinish();
            System.out.println("Playback finished.");
        } catch (IOException e) {
            System.err.println("Error loading frames: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
