package io.tiagovportela.producerconsumer.producers;

import io.tiagovportela.datatypes.FrameData;
import io.tiagovportela.videoproducer.CameraSource;
import io.tiagovportela.producerconsumer.FrameDataQueue;

public class FrameProducer implements Producer {

    private final CameraSource camera;
    private final FrameDataQueue queue;

    public FrameProducer(CameraSource camera, FrameDataQueue frameDataQueue) {
        this.camera = camera;
        this.queue = frameDataQueue;
    }

    @Override
    public void produce() throws InterruptedException {
        while (camera.hasNextFrame()) {
            var frameData = camera.getNextFrame();
            FrameData data = new FrameData(frameData, System.nanoTime(), null, null);
            queue.put(data);
        }
        // Signal end-of-stream to downstream consumer
        queue.put(FrameData.POISON_PILL);
    }
}
