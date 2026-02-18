package io.tiagovportela.producerconsumer.producers;

public interface Producer {
    /**
     * Produces a frame of data.
     *
     * @return the produced frame data
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void produce() throws InterruptedException;
}
