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
     * @param frameIndex      zero-based index of the frame
     * @param latencyNanos    time this frame spent in the pipeline, in nanoseconds
     * @param throughputNanos time since the previous frame completed, in
     *                        nanoseconds
     *                        (0 for the first frame)
     */
    void onFrameProcessed(int frameIndex, long latencyNanos, long throughputNanos);

    /**
     * Records the duration of an individual pipeline stage.
     *
     * @param frameIndex    zero-based index of the frame
     * @param stage         name of the pipeline stage (e.g. "detection", "landmarks")
     * @param durationNanos time the stage took, in nanoseconds
     */
    void recordStage(int frameIndex, String stage, long durationNanos);

    /**
     * Called once after all frames have been processed. Use to flush / close
     * resources.
     */
    void onFinish();
}
