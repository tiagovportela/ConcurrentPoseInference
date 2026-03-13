package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.boudingboxmanager.BoundingBoxManager;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;

/**
 * Second pipeline stage: preprocesses frames and runs person detection.
 * Uses BoundingBoxManager to skip detection when tracking quality is sufficient.
 */
public class FrameConsumer implements Consumer {

    private final FrameDataQueue frameDataQueue;
    private final FrameDataQueue boundingBoxQueue;
    private final FrameMetricsListener metricsListener;
    private final FramePreprocessor preprocessor;
    private final PoseDetector detector;
    private final BoundingBoxManager bboxManager;
    private int index = 0;

    public FrameConsumer(FrameDataQueue frameDataQueue, FrameDataQueue boundingBoxQueue,
                         FrameMetricsListener metricsListener,
                         FramePreprocessor preprocessor, PoseDetector detector,
                         BoundingBoxManager bboxManager) {
        this.frameDataQueue = frameDataQueue;
        this.boundingBoxQueue = boundingBoxQueue;
        this.metricsListener = metricsListener;
        this.preprocessor = preprocessor;
        this.detector = detector;
        this.bboxManager = bboxManager;
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
        boolean skipped;

        if (bboxManager.isToUpdateBoundingBox(t0)) {
            float[] detectorInput = preprocessor.preprocess(frameData.getFrame());
            BoundingBox box = detector.detect(detectorInput);
            frameData.setBoundingBox(box);

            BoundingBox previousCached = bboxManager.getLatestBoundingBox();
            bboxManager.updateBoundingBox(box, false);
            skipped = false;

            // Record IoU between fresh detection and previously cached bbox
            if (previousCached != null && box != null) {
                float iou = BoundingBoxManager.computeIoU(previousCached, box);
                metricsListener.recordStage(index, "bbox_iou", Math.round(iou * 1_000_000.0));
            }

            metricsListener.recordStage(index, "detection_source", 0); // 0 = detector
        } else {
            frameData.setBoundingBox(bboxManager.getLatestBoundingBox());
            skipped = true;
            metricsListener.recordStage(index, "detection_source", 2); // 2 = cached
        }

        long t1 = System.nanoTime();
        frameData.setDetectionSkipped(skipped);
        metricsListener.recordStage(index, "detection", t1 - t0);
        metricsListener.recordStage(index, "detection_skipped", skipped ? 1 : 0);

        frameData.setQueueEntryTime(System.nanoTime());
        boundingBoxQueue.put(frameData);
        index++;
    }
}
