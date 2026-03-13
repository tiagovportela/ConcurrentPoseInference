package io.tiagovportela.inference.boudingboxmanager;

import io.tiagovportela.datatypes.BoundingBox;

/**
 * Manages bounding box caching and skip decisions for the detection stage.
 * <p>
 * When landmark tracking is producing high-quality results (good score, stable IoU),
 * the expensive PoseDetector can be skipped and the cached bounding box reused.
 * </p>
 * <p>
 * All public methods are synchronized for thread safety — the manager is shared
 * between FrameConsumer (reads) and BoundingBoxConsumer (writes).
 * </p>
 */
public class BoundingBoxManager {

    private final long timeThresholdNanos;
    private final float scoreThreshold;
    private final float iouThreshold;

    private BoundingBox latestBoundingBox;
    private long lastUpdateTime;
    private float lastLandmarksAvgScore;
    private BoundingBox previousLandmarkBbox;

    public BoundingBoxManager(long timeThresholdNanos, float scoreThreshold, float iouThreshold) {
        this.timeThresholdNanos = timeThresholdNanos;
        this.scoreThreshold = scoreThreshold;
        this.iouThreshold = iouThreshold;
    }

    /**
     * Decides whether a fresh detection is needed.
     *
     * @param currentTime current {@link System#nanoTime()}
     * @return true if detection should be run, false if cached bbox can be reused
     */
    public synchronized boolean isToUpdateBoundingBox(long currentTime) {
        // First frame — no cache yet
        if (latestBoundingBox == null) {
            return true;
        }
        // Safety net: periodic refresh regardless of tracking quality
        if (lastUpdateTime > 0 && (currentTime - lastUpdateTime) > timeThresholdNanos) {
            return true;
        }
        // Primary decision: score + IoU driven (no timer dependency)
        // If landmark feedback confirms good quality, skip detection
        if (lastLandmarksAvgScore >= scoreThreshold) {
            if (previousLandmarkBbox == null) {
                return false; // good score, no previous bbox to compare — trust it
            }
            float iou = computeIoU(previousLandmarkBbox, latestBoundingBox);
            if (iou >= iouThreshold) {
                return false; // good score + stable bbox — skip
            }
            return true; // fast motion detected (low IoU)
        }
        // No landmark feedback yet, or quality dropped — detect
        return true;
    }

    /**
     * Returns the cached bounding box, or null if none exists.
     */
    public synchronized BoundingBox getLatestBoundingBox() {
        return latestBoundingBox;
    }

    /**
     * Updates the cached bounding box.
     *
     * @param boundingBox the new bounding box
     * @param resetTimer  if true, resets the update timer (used when detection runs
     *                    or when high-quality landmarks confirm the bbox)
     */
    public synchronized void updateBoundingBox(BoundingBox boundingBox, boolean resetTimer) {
        if (boundingBox == null) {
            return;
        }
        this.previousLandmarkBbox = this.latestBoundingBox;
        this.latestBoundingBox = boundingBox;
        if (resetTimer) {
            this.lastUpdateTime = System.nanoTime();
        }
    }

    /**
     * Updates the landmark quality score used in skip decisions.
     *
     * @param avgScore average visibility score across landmarks
     */
    public synchronized void updateLandmarksAvgScore(float avgScore) {
        this.lastLandmarksAvgScore = avgScore;
    }

    /**
     * Computes IoU between two BoundingBox objects.
     */
    public static float computeIoU(BoundingBox a, BoundingBox b) {
        float x1 = Math.max(a.getXMin(), b.getXMin());
        float y1 = Math.max(a.getYMin(), b.getYMin());
        float x2 = Math.min(a.getXMin() + a.getWidth(), b.getXMin() + b.getWidth());
        float y2 = Math.min(a.getYMin() + a.getHeight(), b.getYMin() + b.getHeight());

        float intersection = Math.max(0, x2 - x1) * Math.max(0, y2 - y1);
        float areaA = a.getWidth() * a.getHeight();
        float areaB = b.getWidth() * b.getHeight();
        float union = areaA + areaB - intersection;
        return union > 0 ? intersection / union : 0;
    }
}
