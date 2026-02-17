package io.tiagovportela.datatypes;

import org.opencv.core.Mat;

import java.util.List;

public class FrameData {
    private Mat frame;
    private long timestamp;
    private BoundingBox boundingBox;
    private int landmarks;

    public FrameData(Mat frame, long timestamp, BoundingBox boundingBox, int landmarks) {
        this.frame = frame;
        this.timestamp = timestamp;
        this.boundingBox = boundingBox;
        this.landmarks = landmarks;
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

    public int getLandmarks() {
        return landmarks;
    }
    public void setLandmarks(int landmarks) {
        this.landmarks = landmarks;
    }

}
