package io.tiagovportela.producerconsumer.producers;

import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.FrameDataQueue;
import io.tiagovportela.producerconsumer.consumers.Consumer;
import io.tiagovportela.videoproducer.CameraSource;

/**
 * First pipeline stage: reads frames from the camera source and enqueues them.
 * Implements {@link Consumer} so it can be driven by {@link io.tiagovportela.producerconsumer.PipelineStageThread}.
 */
public class FrameProducer implements Consumer {

    private final CameraSource camera;
    private final FrameDataQueue queue;
    private final FrameMetricsListener metricsListener;
    private int index = 0;

    public FrameProducer(CameraSource camera, FrameDataQueue frameDataQueue, FrameMetricsListener metricsListener) {
        this.camera = camera;
        this.queue = frameDataQueue;
        this.metricsListener = metricsListener;
    }

    @Override
    public void consume() throws InterruptedException {
        if (!camera.hasNextFrame()) {
            queue.put(FrameData.POISON_PILL);
            throw new InterruptedException("End of stream");
        }
        long t0 = System.nanoTime();
        var frameData = camera.getNextFrame();
        long t1 = System.nanoTime();
        FrameData data = new FrameData(frameData, t1, null, null);
        metricsListener.recordStage(index, "frame_acquisition", t1 - t0);
        data.setQueueEntryTime(System.nanoTime());
        queue.put(data);
        index++;
    }
}
