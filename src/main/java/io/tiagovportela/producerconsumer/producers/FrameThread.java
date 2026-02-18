package io.tiagovportela.producerconsumer.producers;

public class FrameThread  implements Runnable{

    private final Producer producer;

    public FrameThread(Producer producer) {
        this.producer = producer;
    }

    @Override
    public void run() {
        try {
            producer.produce();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("FrameThread interrupted: " + e.getMessage());
        }
    }
}
