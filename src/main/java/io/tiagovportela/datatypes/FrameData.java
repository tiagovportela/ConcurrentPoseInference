package io.tiagovportela.datatypes;

import org.opencv.core.Mat;

import java.util.List;

public class FrameData {

    /** Sentinel value signalling end-of-stream through the pipeline. */
    public static final FrameData POISON_PILL = new FrameData(null, -1, null, null);

    private Mat frame;
    private long timestamp;
    private BoundingBox boundingBox;
    private Landmark[] landmarks;
    private volatile long queueEntryTime;
    private boolean detectionSkipped;

    public FrameData(Mat frame, long timestamp, BoundingBox boundingBox, Landmark[] landmarks) {
        this.frame = frame;
        this.timestamp = timestamp;
        this.boundingBox = boundingBox;
        this.landmarks = landmarks;
    }

    /** Returns true if this is the end-of-stream sentinel. */
    public boolean isPoisonPill() {
        return this == POISON_PILL;
    }

    public Mat getFrame() {
        return frame;
    }

    public void setFrame(Mat frame) {
        this.frame = frame;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public Landmark[] getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(Landmark[] landmarks) {
        this.landmarks = landmarks;
    }

    public long getQueueEntryTime() {
        return queueEntryTime;
    }

    public void setQueueEntryTime(long queueEntryTime) {
        this.queueEntryTime = queueEntryTime;
    }

    public boolean isDetectionSkipped() {
        return detectionSkipped;
    }

    public void setDetectionSkipped(boolean detectionSkipped) {
        this.detectionSkipped = detectionSkipped;
    }

}
