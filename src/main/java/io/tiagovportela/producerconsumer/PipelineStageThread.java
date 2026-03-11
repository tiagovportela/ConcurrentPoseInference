package io.tiagovportela.producerconsumer;

import io.tiagovportela.producerconsumer.consumers.Consumer;
import io.tiagovportela.producerconsumer.producers.Producer;

/**
 * Generic thread wrapper for any pipeline stage.
 * Repeatedly calls {@link Producer#produce()} or {@link Consumer#consume()}
 * until the stage signals end-of-stream via {@link InterruptedException}.
 */
public class PipelineStageThread implements Runnable {

    private final Runnable stageAction;
    private final String name;

    public PipelineStageThread(Producer stage, String name) {
        this.name = name;
        this.stageAction = () -> {
            try {
                stage.produce();
            } catch (InterruptedException e) {
                throw new StageInterruptedException(e);
            }
        };
    }

    public PipelineStageThread(Consumer stage, String name) {
        this.name = name;
        this.stageAction = () -> {
            try {
                stage.consume();
            } catch (InterruptedException e) {
                throw new StageInterruptedException(e);
            }
        };
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                stageAction.run();
            }
        } catch (StageInterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Internal unchecked wrapper so the lambda can propagate InterruptedException. */
    private static class StageInterruptedException extends RuntimeException {
        StageInterruptedException(InterruptedException cause) {
            super(cause);
        }
    }
}
