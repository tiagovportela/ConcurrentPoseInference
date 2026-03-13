package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.boudingboxmanager.BoundingBoxManager;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import org.opencv.core.Mat;

/**
 * Third pipeline stage: estimates pose landmarks from the detected bounding box.
 * Feeds landmark-derived bounding boxes back to the BoundingBoxManager.
 */
public class BoundingBoxConsumer implements Consumer {

    private final FrameDataQueue boundingBoxQueue;
    private final FrameDataQueue landmarksQueue;
    private final FrameMetricsListener metricsListener;
    private final PoseTracker tracker;
    private final BoundingBoxManager bboxManager;
    private final float scoreThreshold;
    private int index = 0;

    public BoundingBoxConsumer(FrameDataQueue boundingBoxQueue, FrameDataQueue landmarksQueue,
                               FrameMetricsListener metricsListener, PoseTracker tracker,
                               BoundingBoxManager bboxManager, float scoreThreshold) {
        this.boundingBoxQueue = boundingBoxQueue;
        this.landmarksQueue = landmarksQueue;
        this.metricsListener = metricsListener;
        this.tracker = tracker;
        this.bboxManager = bboxManager;
        this.scoreThreshold = scoreThreshold;
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

        // Feed landmark-derived bbox back to the manager
        if (landmarks != null) {
            float avgScore = computeAvgVisibility(landmarks);
            bboxManager.updateLandmarksAvgScore(avgScore);

            BoundingBox landmarkBbox = deriveBoundingBox(landmarks);
            if (landmarkBbox != null) {
                // Reset timer when landmarks are high quality — allows indefinite skip runs
                boolean resetTimer = avgScore >= scoreThreshold;
                bboxManager.updateBoundingBox(landmarkBbox, resetTimer);

                String source = resetTimer ? "landmarks_good" : "landmarks_weak";
                metricsListener.recordStage(index, "detection_source", resetTimer ? 1 : 3);
            }

            metricsListener.recordStage(index, "landmarks_avg_score",
                    Math.round(avgScore * 1_000_000.0));
        }

        frameData.setQueueEntryTime(System.nanoTime());
        landmarksQueue.put(frameData);
        System.out.println("Processed frame " + index);
        index++;
    }

    private static float computeAvgVisibility(Landmark[] landmarks) {
        float sum = 0;
        for (Landmark lm : landmarks) {
            sum += lm.getVisibility();
        }
        return sum / landmarks.length;
    }

    /**
     * Derives a bounding box from landmarks by computing the tight enclosing rect
     * with a small margin.
     */
    private static BoundingBox deriveBoundingBox(Landmark[] landmarks) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        for (Landmark lm : landmarks) {
            minX = Math.min(minX, lm.getX());
            minY = Math.min(minY, lm.getY());
            maxX = Math.max(maxX, lm.getX());
            maxY = Math.max(maxY, lm.getY());
        }

        float w = maxX - minX;
        float h = maxY - minY;
        if (w <= 0 || h <= 0) {
            return null;
        }

        // Add 10% margin
        float marginX = w * 0.1f;
        float marginY = h * 0.1f;
        float xMin = Math.max(0f, minX - marginX);
        float yMin = Math.max(0f, minY - marginY);
        float boxW = Math.min(w + 2 * marginX, 1f - xMin);
        float boxH = Math.min(h + 2 * marginY, 1f - yMin);

        // Use average visibility as the score
        float avgScore = computeAvgVisibility(landmarks);
        return new BoundingBox(xMin, yMin, boxW, boxH, avgScore);
    }
}
