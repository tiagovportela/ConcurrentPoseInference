package io.tiagovportela.producerconsumer;

import io.tiagovportela.producerconsumer.consumers.Consumer;

/**
 * Generic thread wrapper for any pipeline stage.
 * Repeatedly calls {@link Consumer#consume()} until the stage signals
 * end-of-stream via {@link InterruptedException}.
 */
public class PipelineStageThread implements Runnable {

    private final Consumer stage;
    private final String name;

    public PipelineStageThread(Consumer stage, String name) {
        this.stage = stage;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                stage.consume();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
