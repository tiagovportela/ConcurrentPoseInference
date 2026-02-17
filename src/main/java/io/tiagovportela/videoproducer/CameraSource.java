package io.tiagovportela.videoproducer;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Simulates a camera feed by reading video frames from a directory of PNG
 * images.
 * Supports both pull-based access via {@link #getNextFrame()} and push-based
 * delivery via {@link #startPlayback(FrameCallback)}.
 */
public class CameraSource {

    /**
     * Callback interface for receiving frames in push-based playback mode.
     */
    @FunctionalInterface
    public interface FrameCallback {
        void onFrameAvailable(Mat frame, int frameIndex);
    }

    private final List<Mat> frames;
    private int currentFrameIndex;

    /**
     * Creates a CameraSource that loads all {@code img_*.png} files from the given
     * directory.
     *
     * @param framesDirectory the directory containing the frame images
     * @throws IOException              if a frame image cannot be read
     * @throws IllegalArgumentException if the directory is invalid or contains no
     *                                  frames
     */
    public CameraSource(File framesDirectory) throws IOException {
        if (framesDirectory == null || !framesDirectory.isDirectory()) {
            throw new IllegalArgumentException("Frames directory is invalid: " + framesDirectory);
        }

        File[] frameFiles = framesDirectory.listFiles((dir, name) -> name.startsWith("img_") && name.endsWith(".png"));

        if (frameFiles == null || frameFiles.length == 0) {
            throw new IllegalArgumentException("No frame images found in: " + framesDirectory);
        }

        Arrays.sort(frameFiles, Comparator.comparing(File::getName));

        this.frames = new ArrayList<>(frameFiles.length);
        for (File file : frameFiles) {
            Mat image = Imgcodecs.imread(file.getAbsolutePath());
            if (image.empty()) {
                throw new IOException("Failed to read image: " + file.getAbsolutePath());
            }
            frames.add(image);
        }

        this.currentFrameIndex = 0;
    }

    /**
     * Returns the next frame, or {@code null} if all frames have been consumed.
     */
    public Mat getNextFrame() {
        if (!hasNextFrame()) {
            return null;
        }
        return frames.get(currentFrameIndex++);
    }

    /**
     * Returns {@code true} if there are remaining frames to read.
     */
    public boolean hasNextFrame() {
        return currentFrameIndex < frames.size();
    }

    /**
     * Resets the playback position to the first frame.
     */
    public void reset() {
        currentFrameIndex = 0;
    }

    /**
     * Returns the total number of loaded frames.
     */
    public int getTotalFrames() {
        return frames.size();
    }

    /**
     * Iterates through all frames from the current position and invokes the
     * callback for each one.
     *
     * @param callback the callback to receive each frame
     */
    public void startPlayback(FrameCallback callback) {
        while (hasNextFrame()) {
            int index = currentFrameIndex;
            Mat frame = getNextFrame();
            callback.onFrameAvailable(frame, index);
        }
    }
}
