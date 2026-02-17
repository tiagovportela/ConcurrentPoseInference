package io.tiagovportela;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FpsCsvExporter;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.videoproducer.CameraSource;
import nu.pattern.OpenCV;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Load OpenCV native library (required before any OpenCV API call)
        OpenCV.loadLocally();

        File framesDir = new File("src/main/java/io/tiagovportela/videoproducer/frames");
        FramePreprocessor preprocessor = new FramePreprocessor(224);
        PoseDetector detector = new PoseDetector("src/main/resources/models/pose_detection.tflite");
        PoseTracker tracker = new PoseTracker("src/main/resources/pose_landmark_heavy.tflite");

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

                // Stage 1: Detect person bounding box
                float[] detectorInput = preprocessor.preprocess(frame);
                BoundingBox box = detector.detect(detectorInput);

                if (box != null) {
                    System.out.println(box);

                    // Stage 2: Estimate landmarks (tracker handles crop + preprocess)
                    Landmark[] landmarks = tracker.track(frame, box);
                    if (landmarks != null) {
                        for (int i = 0; i < landmarks.length; i++) {
                            System.out.printf("  Landmark %2d: %s%n", i, landmarks[i]);
                        }
                    }
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
