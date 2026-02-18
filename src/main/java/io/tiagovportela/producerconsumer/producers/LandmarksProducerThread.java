package io.tiagovportela.producerconsumer.producers;

import io.tiagovportela.metrics.FrameMetricsListener;
import io.tiagovportela.producerconsumer.consumers.Consumer;

public class LandmarksProducerThread implements Runnable{

    private final Consumer consumer;


    public LandmarksProducerThread(Consumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                consumer.consume();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("LandmarksProducerThread interrupted: " + e.getMessage());
        }
    }
}
