package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.datatypes.Landmark;
import io.tiagovportela.inference.posetracker.PoseTracker;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import org.opencv.core.Mat;

public class BoundingBoxConsumer implements Consumer {
    private final FrameDataQueue boundingBoxQueue;
    private final FrameDataQueue landmarksQueue;
    private final PoseTracker tracker = new PoseTracker("src/main/resources/models/pose_landmark_heavy.tflite");
    private static int index = 0;

    public BoundingBoxConsumer(FrameDataQueue boundingBoxQueue, FrameDataQueue landmarksQueue) {
        this.boundingBoxQueue = boundingBoxQueue;
        this.landmarksQueue = landmarksQueue;
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
        System.out.println("Processed frame " + index);
        index++;
    }
}
