package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import org.opencv.core.Mat;

public class BoundingBoxConsumer implements Consumer {
    private final FrameDataQueue boundingBoxQueue;
    private final FrameDataQueue landmarksQueue;
    private final PoseTracker tracker = new PoseTracker("src/main/resources/models/pose_landmark_heavy.tflite");
    private final FrameMetricsListener metrics;
    private long lastFrameTime = 0;
    private static int index = 0;

    public BoundingBoxConsumer(FrameDataQueue boundingBoxQueue, FrameDataQueue landmarksQueue,
            FrameMetricsListener metricsListener) {
        this.boundingBoxQueue = boundingBoxQueue;
        this.landmarksQueue = landmarksQueue;
        this.metrics = metricsListener;
    }

    @Override
    public void consume() throws InterruptedException {
        FrameData frameData = boundingBoxQueue.take();
        if (frameData.isPoisonPill()) {
            landmarksQueue.put(FrameData.POISON_PILL);
            throw new InterruptedException("End of stream");
        }
        Mat frame = frameData.getFrame();
        BoundingBox box = frameData.getBoundingBox();
        Landmark[] landmarks = tracker.track(frame, box);
        frameData.setLandmarks(landmarks);
        landmarksQueue.put(frameData);

        long now = System.nanoTime();

        // Latency: time this frame spent in the pipeline (born → done)
        long latencyNanos = now - frameData.getTimestamp();

        // Throughput: time since the last frame completed
        long throughputNanos = (lastFrameTime != 0) ? (now - lastFrameTime) : 0;
        lastFrameTime = now;

        metrics.onFrameProcessed(index, latencyNanos, throughputNanos);
        System.out.println("Processed frame " + index);
        index++;
    }
}
