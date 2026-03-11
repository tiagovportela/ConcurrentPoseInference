package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import io.tiagovportela.visualization.PoseVisualizer;
import org.opencv.core.Mat;

/**
 * Fourth (final) pipeline stage: draws visualisation and records metrics.
 */
public class LandmarksConsumer implements Consumer {

    private final FrameDataQueue landmarksQueue;
    private final PoseVisualizer visualizer;
    private final FrameMetricsListener metricsListener;
    private int index = 0;
    private long previousCompletionTime = 0;

    public LandmarksConsumer(FrameDataQueue landmarksQueue, PoseVisualizer visualizer, FrameMetricsListener metricsListener) {
        this.landmarksQueue = landmarksQueue;
        this.visualizer = visualizer;
        this.metricsListener = metricsListener;
    }

    @Override
    public void consume() throws InterruptedException {
        FrameData frameData = landmarksQueue.take();
        if (frameData.isPoisonPill()) {
            throw new InterruptedException("End of stream");
        }
        long afterTake = System.nanoTime();
        metricsListener.recordStage(index, "queue_landmarks", afterTake - frameData.getQueueEntryTime());

        Mat frame = frameData.getFrame();
        BoundingBox box = frameData.getBoundingBox();
        Landmark[] landmarks = frameData.getLandmarks();

        long t0 = System.nanoTime();
        visualizer.draw(frame, landmarks, box, index);
        long t1 = System.nanoTime();

        long latency = t1 - frameData.getTimestamp();
        long throughput = (index == 0) ? 0 : (t1 - previousCompletionTime);
        previousCompletionTime = t1;

        metricsListener.onFrameProcessed(index, latency, throughput);
        metricsListener.recordStage(index, "visualization", t1 - t0);
        System.out.println("Visualized frame " + index);
        index++;
    }
}
