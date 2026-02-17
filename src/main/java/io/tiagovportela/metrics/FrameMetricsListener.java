package io.tiagovportela.metrics;

/**
 * Pluggable listener for per-frame processing metrics.
 * Implement this interface to consume timing data from the playback loop.
 */
public interface FrameMetricsListener {

    /** Called once before playback begins. */
    void onStart();

    /**
     * Called after each frame has been processed.
     *
     * @param frameIndex   zero-based index of the frame
     * @param elapsedNanos time taken to process this frame, in nanoseconds
     */
    void onFrameProcessed(int frameIndex, long elapsedNanos);

    /**
     * Called once after all frames have been processed. Use to flush / close
     * resources.
     */
    void onFinish();
}
