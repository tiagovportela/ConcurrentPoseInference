package io.tiagovportela;

import io.tiagovportela.videoproducer.CameraSource;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        // Resolve the frames directory relative to the source tree
        File framesDir = new File("src/main/java/io/tiagovportela/videoproducer/frames");

        try {
            CameraSource camera = new CameraSource(framesDir);
            System.out.println("Loaded " + camera.getTotalFrames() + " frames.");

            // Push-based playback: iterate every frame via callback
            camera.startPlayback((frame, index) -> {
                System.out.printf("Frame %04d: %dx%d%n",
                        index, frame.getWidth(), frame.getHeight());
            });

            System.out.println("Playback finished.");
        } catch (IOException e) {
            System.err.println("Error loading frames: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
