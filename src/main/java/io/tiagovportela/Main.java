package io.tiagovportela;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
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
        // Load OpenCV native library (required before any OpenCV API call)
        OpenCV.loadLocally();

        File framesDir = new File("src/main/java/io/tiagovportela/videoproducer/frames");

        FrameMetricsListener metricsListener = new FpsCsvExporter("fps_metrics.csv");
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
            BoundingBoxConsumer boundingBoxConsumer = new BoundingBoxConsumer(boundingBoxQueue, landmarksQueue,
                    metricsListener);
            executor.execute(new LandmarksProducerThread(boundingBoxConsumer));

            // Thread 4: consume landmarks -> visualize and save
            LandmarksConsumer landmarksConsumer = new LandmarksConsumer(landmarksQueue, visualizer);
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
}
