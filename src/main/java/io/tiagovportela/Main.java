package io.tiagovportela;

import io.tiagovportela.config.PipelineConfig;
import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.boundingboxmanager.BoundingBoxManager;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FpsCsvExporter;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import io.tiagovportela.producerconsumer.PipelineStageThread;
import io.tiagovportela.producerconsumer.consumers.BoundingBoxConsumer;
import io.tiagovportela.producerconsumer.producers.BoundingBoxProducer;
import io.tiagovportela.videoproducer.CameraSource;
import io.tiagovportela.visualization.PoseVisualizer;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {


        OpenCV.loadLocally();

        new Main().multiThreadedRun();
        new Main().singleThreadedRun();
    }

    public void multiThreadedRun() {
        PipelineConfig config = PipelineConfig.builder()
                .useMultithreading(true)
                .fpsMetricsFile("results/fps_metrics_multithread_2_stages_bbmanagers.csv")
                .stageMetricsFile("results/pipeline_stage_metrics_multithread_2_stages_bbmanagers.csv")
                .threadPoolSize(2)
                .build();
        FrameMetricsListener metrics = new FpsCsvExporter(config.getFpsMetricsFile(), config.getStageMetricsFile());
        PoseVisualizer visualizer = new PoseVisualizer(config.getOutputDir());
        FramePreprocessor preprocessor = new FramePreprocessor(config.getDetectorInputSize());
        PoseDetector detector = new PoseDetector(config.getPoseDetectionModelPath());
        PoseTracker tracker = new PoseTracker(config.getPoseLandmarkModelPath());

        try {
            CameraSource camera = new CameraSource(new File(config.getFramesDir()));
            System.out.println("Loaded " + camera.getTotalFrames() + " frames.");

            FrameDataQueue dataQueue = new FrameDataQueue(config.getQueueCapacity());
            BoundingBoxManager bboxManager = new BoundingBoxManager();

            metrics.onStart();

            ExecutorService executor = Executors.newFixedThreadPool(config.getThreadPoolSize());
            executor.execute(new PipelineStageThread(
                    new BoundingBoxProducer(
                            camera, dataQueue, preprocessor, metrics, detector, bboxManager),
                    "BoundingBoxProducer"));
            executor.execute(new PipelineStageThread(
                    new BoundingBoxConsumer(
                            dataQueue, metrics, tracker, bboxManager, visualizer),
                    "BoundingBoxConsumer"));
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                System.err.println("Pipeline did not terminate within timeout.");
                executor.shutdownNow();
            }

            metrics.onFinish();
            System.out.println("Playback finished.");
        } catch (IOException | InterruptedException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void singleThreadedRun() {
        PipelineConfig config = PipelineConfig.builder()
                .useMultithreading(true)
                .fpsMetricsFile("results/fps_metrics_singlethread_2_stages.csv")
                .stageMetricsFile("results/pipeline_stage_metrics_singlethread_2_stages.csv")
                .build();
        FramePreprocessor preprocessor = new FramePreprocessor(config.getDetectorInputSize());
        PoseDetector detector = new PoseDetector(config.getPoseDetectionModelPath());
        PoseTracker tracker = new PoseTracker(config.getPoseLandmarkModelPath());
        PoseVisualizer visualizer = new PoseVisualizer(config.getOutputDir());
        FrameMetricsListener metrics = new FpsCsvExporter(config.getFpsMetricsFile(), config.getStageMetricsFile());

        try {
            CameraSource camera = new CameraSource(new File(config.getFramesDir()));
            System.out.println("Loaded " + camera.getTotalFrames() + " frames.");

            metrics.onStart();

            long previousCompletionTime = 0;
            int index = 0;

            while (camera.hasNextFrame()) {
                long a0 = System.nanoTime();
                Mat frame = camera.getNextFrame();
                long a1 = System.nanoTime();
                metrics.recordStage(index, "frame_acquisition", a1 - a0);

                long t0 = System.nanoTime();

                System.out.printf("Frame %04d: %dx%d%n", index, frame.cols(), frame.rows());

                long s1 = System.nanoTime();
                float[] detectorInput = preprocessor.preprocess(frame);
                BoundingBox box = detector.detect(detectorInput);
                long s2 = System.nanoTime();
                metrics.recordStage(index, "detection", s2 - s1);

                if (box != null) {
                    System.out.println(box);

                    long s3 = System.nanoTime();
                    Landmark[] landmarks = tracker.track(frame, box);
                    long s4 = System.nanoTime();
                    metrics.recordStage(index, "landmarks", s4 - s3);

                    long s5 = System.nanoTime();
                    visualizer.draw(frame, landmarks, box, index);
                    long s6 = System.nanoTime();
                    metrics.recordStage(index, "visualization", s6 - s5);
                }

                long t1 = System.nanoTime();
                long latency = t1 - t0;
                long throughput = (index == 0) ? 0 : (t1 - previousCompletionTime);
                previousCompletionTime = t1;

                metrics.onFrameProcessed(index, latency, throughput);
                index++;
            }

            metrics.onFinish();
            System.out.println("Playback finished.");
        } catch (IOException e) {
            System.err.println("Error loading frames: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
