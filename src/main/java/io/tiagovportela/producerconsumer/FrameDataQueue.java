package io.tiagovportela.producerconsumer;

import io.tiagovportela.datatypes.FrameData;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thread-safe queue for passing {@link FrameData} between pipeline stages.
 *
 * <p>
 * Uses a <b>conflated</b> strategy: when the queue is full, the oldest
 * frame is dropped to make room for the freshest one. This prevents the
 * producer from blocking and keeps latency low in real-time scenarios.
 * </p>
 */
public class FrameDataQueue {
    private final BlockingQueue<FrameData> queue;

    public FrameDataQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    /**
     * Inserts a frame into the queue.
     * If the queue is full, the oldest (stale) frame is dropped first
     * so the producer never blocks.
     */
    public void put(FrameData frame) {
        // If the queue is full, drop the oldest frame to make room
        while (!queue.offer(frame)) {
            queue.poll(); // discard stale frame
        }
    }

    /**
     * Retrieves and removes the head of the queue, blocking if empty.
     */
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