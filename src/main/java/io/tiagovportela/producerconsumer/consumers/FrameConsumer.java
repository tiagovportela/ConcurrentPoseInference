package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.producerconsumer.FrameDataQueue;

public class FrameConsumer implements Consumer {

    private final FrameDataQueue frameDataQueue;
    private final FrameDataQueue boundingBoxQueue;
    FramePreprocessor preprocessor = new FramePreprocessor(224);
    PoseDetector detector = new PoseDetector("src/main/resources/models/pose_detection.tflite");

    public FrameConsumer(FrameDataQueue frameDataQueue, FrameDataQueue boundingBoxQueue) {
        this.frameDataQueue = frameDataQueue;
        this.boundingBoxQueue = boundingBoxQueue;
    }

    @Override
    public void consume() throws InterruptedException {
        FrameData frameData = frameDataQueue.take();
        if (frameData.isPoisonPill()) {
            boundingBoxQueue.put(FrameData.POISON_PILL);
            throw new InterruptedException("End of stream");
        }
        float[] detectorInput = preprocessor.preprocess(frameData.getFrame());
        BoundingBox box = detector.detect(detectorInput);
        frameData.setBoundingBox(box);
        boundingBoxQueue.put(frameData);
    }

}
