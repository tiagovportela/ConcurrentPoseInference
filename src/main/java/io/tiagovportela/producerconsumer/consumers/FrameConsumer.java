package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;

/**
 * Second pipeline stage: preprocesses frames and runs person detection.
 */
public class FrameConsumer implements Consumer {

    private final FrameDataQueue frameDataQueue;
    private final FrameDataQueue boundingBoxQueue;
    private final FrameMetricsListener metricsListener;
    private final FramePreprocessor preprocessor;
    private final PoseDetector detector;
    private int index = 0;

    public FrameConsumer(FrameDataQueue frameDataQueue, FrameDataQueue boundingBoxQueue,
                         FrameMetricsListener metricsListener,
                         FramePreprocessor preprocessor, PoseDetector detector) {
        this.frameDataQueue = frameDataQueue;
        this.boundingBoxQueue = boundingBoxQueue;
        this.metricsListener = metricsListener;
        this.preprocessor = preprocessor;
        this.detector = detector;
    }

    @Override
    public void consume() throws InterruptedException {
        FrameData frameData = frameDataQueue.take();
        if (frameData.isPoisonPill()) {
            boundingBoxQueue.put(FrameData.POISON_PILL);
            throw new InterruptedException("End of stream");
        }
        long afterTake = System.nanoTime();
        metricsListener.recordStage(index, "queue_frame", afterTake - frameData.getQueueEntryTime());

        long t0 = System.nanoTime();
        float[] detectorInput = preprocessor.preprocess(frameData.getFrame());
        BoundingBox box = detector.detect(detectorInput);
        long t1 = System.nanoTime();
        frameData.setBoundingBox(box);
        metricsListener.recordStage(index, "detection", t1 - t0);

        frameData.setQueueEntryTime(System.nanoTime());
        boundingBoxQueue.put(frameData);
        index++;
    }
}
