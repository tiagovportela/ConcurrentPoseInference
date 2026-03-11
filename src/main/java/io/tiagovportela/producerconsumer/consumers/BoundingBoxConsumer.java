package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import org.opencv.core.Mat;

/**
 * Third pipeline stage: estimates pose landmarks from the detected bounding box.
 */
public class BoundingBoxConsumer implements Consumer {

    private final FrameDataQueue boundingBoxQueue;
    private final FrameDataQueue landmarksQueue;
    private final FrameMetricsListener metricsListener;
    private final PoseTracker tracker;
    private int index = 0;

    public BoundingBoxConsumer(FrameDataQueue boundingBoxQueue, FrameDataQueue landmarksQueue,
                               FrameMetricsListener metricsListener, PoseTracker tracker) {
        this.boundingBoxQueue = boundingBoxQueue;
        this.landmarksQueue = landmarksQueue;
        this.metricsListener = metricsListener;
        this.tracker = tracker;
    }

    @Override
    public void consume() throws InterruptedException {
        FrameData frameData = boundingBoxQueue.take();
        if (frameData.isPoisonPill()) {
            landmarksQueue.put(FrameData.POISON_PILL);
            throw new InterruptedException("End of stream");
        }
        long afterTake = System.nanoTime();
        metricsListener.recordStage(index, "queue_bbox", afterTake - frameData.getQueueEntryTime());

        long t0 = System.nanoTime();
        Mat frame = frameData.getFrame();
        BoundingBox box = frameData.getBoundingBox();
        Landmark[] landmarks = (box != null) ? tracker.track(frame, box) : null;
        long t1 = System.nanoTime();
        frameData.setLandmarks(landmarks);
        metricsListener.recordStage(index, "landmarks", t1 - t0);

        frameData.setQueueEntryTime(System.nanoTime());
        landmarksQueue.put(frameData);
        System.out.println("Processed frame " + index);
        index++;
    }
}
