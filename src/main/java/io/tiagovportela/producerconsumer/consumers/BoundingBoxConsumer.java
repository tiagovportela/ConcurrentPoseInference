package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.boundingboxmanager.BoundingBoxManager;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import io.tiagovportela.visualization.PoseVisualizer;
import org.opencv.core.Mat;

public class BoundingBoxConsumer implements Consumer {
    private final FrameDataQueue queue;
    private final FrameMetricsListener metricsListener;
    private final PoseTracker tracker;
    private final BoundingBoxManager bboxManager;
    private final PoseVisualizer visualizer;
    private int index = 0;
    private long previousCompletionTime = 0;

    public BoundingBoxConsumer(FrameDataQueue queue, FrameMetricsListener metricsListener, PoseTracker tracker, BoundingBoxManager bboxManager, PoseVisualizer visualizer) {
        this.queue = queue;
        this.metricsListener = metricsListener;
        this.tracker = tracker;
        this.bboxManager = bboxManager;

        this.visualizer = visualizer;
    }

    @Override
    public void consume() throws InterruptedException {
        FrameData frameData = queue.take();
        if(frameData.isPoisonPill()) {
            throw new InterruptedException("End of stream");
        }
        long afterTake = System.nanoTime();
        metricsListener.recordStage(index, "data_queue", afterTake - frameData.getQueueEntryTime());

        long t0 = System.nanoTime();
        Mat frame = frameData.getFrame();
        BoundingBox box = frameData.getBoundingBox();
        Landmark[] landmarks = (box != null) ? tracker.track(frame, box) : null;
        bboxManager.updateLandmarksAvgScore(getLandmarksAvgScore(landmarks));
        long t1 = System.nanoTime();
        visualizer.draw(frame, landmarks, box, index);
        long t2 = System.nanoTime();
        
        long latency = t2 - frameData.getTimestamp();
        long throughput = (index == 0) ? 0 : (t2 - previousCompletionTime);
        previousCompletionTime = t2;

        metricsListener.onFrameProcessed(index, latency, throughput);
        metricsListener.recordStage(index, "landmarks", t1 - t0);
        metricsListener.recordStage(index, "visualization", t2 - t1);
        System.out.println("Visualized frame " + index);
        index++;
    }

    public float getLandmarksAvgScore(Landmark[] landmarks) {
        if (landmarks == null || landmarks.length == 0) {
            return 0.0f;
        }
        float sum = 0.0f;
        for (Landmark lm : landmarks) {
            sum += lm.getVisibility();
        }
        return sum / landmarks.length;
    }
}
