package io.tiagovportela.producerconsumer.producers;

public interface Producer {
    /**
     * Produces a frame of data and enqueues it for downstream processing.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void produce() throws InterruptedException;
}

