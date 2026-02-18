package io.tiagovportela.producerconsumer.consumers;

public class BoundingBoxThread  implements Runnable{

    private final Consumer consumer;

    public BoundingBoxThread(Consumer consumer) {
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
            System.err.println("BoundingBoxThread interrupted: " + e.getMessage());
        }
    }
}
