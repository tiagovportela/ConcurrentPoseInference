package io.tiagovportela;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FpsCsvExporter;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import io.tiagovportela.producerconsumer.consumers.BoundingBoxConsumer;
import io.tiagovportela.producerconsumer.consumers.BoundingBoxThread;
import io.tiagovportela.producerconsumer.consumers.FrameConsumer;
import io.tiagovportela.producerconsumer.consumers.LandmarksConsumer;
import io.tiagovportela.producerconsumer.consumers.LandmarksConsumerThread;
import io.tiagovportela.producerconsumer.producers.FrameProducer;
import io.tiagovportela.producerconsumer.producers.FrameThread;
import io.tiagovportela.producerconsumer.producers.LandmarksProducerThread;
import io.tiagovportela.videoproducer.CameraSource;
import io.tiagovportela.visualization.PoseVisualizer;
import nu.pattern.OpenCV;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        boolean useMultithreading = true; // Set to false to run single-threaded version
        if (useMultithreading) {
            new Main().multiThreadedRun();
        } else {
            new Main().singleThreadedRun();
        }
    }

    public void multiThreadedRun() {
        // Load OpenCV native library (required before any OpenCV API call)
        OpenCV.loadLocally();

        File framesDir = new File("src/main/java/io/tiagovportela/videoproducer/frames");

        FrameMetricsListener metricsListener = new FpsCsvExporter("fps_metrics_multithread.csv");
        PoseVisualizer visualizer = new PoseVisualizer("output");

        try {
            CameraSource camera = new CameraSource(framesDir);
            ExecutorService executor = Executors.newFixedThreadPool(4);
            System.out.println("Loaded " + camera.getTotalFrames() + " frames.");

            FrameDataQueue frameQueue = new FrameDataQueue(2);
            FrameDataQueue boundingBoxQueue = new FrameDataQueue(2);
            FrameDataQueue landmarksQueue = new FrameDataQueue(2);

            metricsListener.onStart();

            // Thread 1: produce frames
            FrameProducer frameProducer = new FrameProducer(camera, frameQueue);
            FrameThread frameProducerThread = new FrameThread(frameProducer);
            executor.execute(frameProducerThread);

            // Thread 2: consume frames -> produce bounding boxes
            FrameConsumer frameConsumer = new FrameConsumer(frameQueue, boundingBoxQueue);
            executor.execute(new BoundingBoxThread(frameConsumer));

            // Thread 3: consume bounding boxes -> produce landmarks
            BoundingBoxConsumer boundingBoxConsumer = new BoundingBoxConsumer(boundingBoxQueue, landmarksQueue);
            executor.execute(new LandmarksProducerThread(boundingBoxConsumer));

            // Thread 4: consume landmarks -> visualize, save, and record metrics
            LandmarksConsumer landmarksConsumer = new LandmarksConsumer(landmarksQueue, visualizer, metricsListener);
            executor.execute(new LandmarksConsumerThread(landmarksConsumer));

            // Wait for all threads to finish (poison pill propagates termination)
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.MINUTES);

            metricsListener.onFinish();
            System.out.println("Playback finished.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void singleThreadedRun() {
        OpenCV.loadLocally();

        File framesDir = new File("src/main/java/io/tiagovportela/videoproducer/frames");
        FramePreprocessor preprocessor = new FramePreprocessor(224);
        PoseDetector detector = new PoseDetector("src/main/resources/models/pose_detection.tflite");
        PoseTracker tracker = new PoseTracker("src/main/resources/models/pose_landmark_heavy.tflite");
        PoseVisualizer visualizer = new PoseVisualizer("output");

        FrameMetricsListener metricsListener = new FpsCsvExporter("fps_metrics_single_thread.csv");

        try {
            CameraSource camera = new CameraSource(framesDir);
            System.out.println("Loaded " + camera.getTotalFrames() + " frames.");

            metricsListener.onStart();

            final long[] previousCompletionTime = { 0 };

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

                    // Stage 3: Visualize and save annotated frame
                    visualizer.draw(frame, landmarks, box, index);
                }

                long t1 = System.nanoTime();
                long latency = t1 - t0;
                long throughput = (index == 0) ? 0 : (t1 - previousCompletionTime[0]);
                previousCompletionTime[0] = t1;

                metricsListener.onFrameProcessed(index, latency, throughput);
            });

            metricsListener.onFinish();
            System.out.println("Playback finished.");
        } catch (IOException e) {
            System.err.println("Error loading frames: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
