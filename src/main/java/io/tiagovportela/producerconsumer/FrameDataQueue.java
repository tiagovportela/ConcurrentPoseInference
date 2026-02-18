package io.tiagovportela.producerconsumer;

import io.tiagovportela.datatypes.FrameData;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FrameDataQueue {
    private final BlockingQueue<FrameData> queue;

    public FrameDataQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    public void put(FrameData frame) throws InterruptedException {
        queue.put(frame);
    }

    public FrameData take() throws InterruptedException {
        return queue.take();
    }

    public FrameData poll() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}