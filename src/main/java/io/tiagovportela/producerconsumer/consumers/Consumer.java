package io.tiagovportela.producerconsumer.consumers;

import io.tiagovportela.datatypes.FrameData;

public interface Consumer {
    /**
     * Consumes a frame of data.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    void consume() throws InterruptedException;
}
