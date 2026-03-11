package io.tiagovportela.producerconsumer.producers;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.inference.FramePreprocessor;
import io.tiagovportela.inference.boundingboxmanager.BoundingBoxManager;
import io.tiagovportela.inference.posedetector.PoseDetector;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import io.tiagovportela.videoproducer.CameraSource;

public class BoundingBoxProducer implements Producer {
    private final CameraSource camera;
    private final FrameDataQueue queue;
    private final FramePreprocessor preprocessor;
    private final FrameMetricsListener metricsListener;
    private final PoseDetector detector;
    private final BoundingBoxManager bboxManager;
    private int index = 0;

    public BoundingBoxProducer(CameraSource camera, FrameDataQueue frameDataQueue, FramePreprocessor preprocessor, FrameMetricsListener metricsListener, PoseDetector detector, BoundingBoxManager bboxManager) {
        this.camera = camera;
        this.queue = frameDataQueue;
        this.preprocessor = preprocessor;
        this.metricsListener = metricsListener;
        this.detector = detector;
        this.bboxManager = bboxManager;
    }

    @Override
    public void produce() throws InterruptedException {
        if (!camera.hasNextFrame()) {
            queue.put(FrameData.POISON_PILL);
            throw new InterruptedException("End of stream");
        }
        long t0 = System.nanoTime();
        var frame = camera.getNextFrame();
        float[] detectorInput = preprocessor.preprocess(frame);
        if(bboxManager.isToUpdateBoundingBox(System.nanoTime())) {
            BoundingBox box = detector.detect(detectorInput);
            if (box != null) {
                bboxManager.updateBoundingBox(box);
            }

        }
        long t1 = System.nanoTime();
        FrameData data = new FrameData(frame, t1, bboxManager.getLatestBoundingBox(), null);
        data.setBoundingBox(bboxManager.getLatestBoundingBox());
        metricsListener.recordStage(index, "bbox_calculation", t1 - t0);
        data.setQueueEntryTime(System.nanoTime());
        queue.put(data);
        index++;
    }
}
