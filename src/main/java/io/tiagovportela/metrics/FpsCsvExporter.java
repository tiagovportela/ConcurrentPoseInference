package io.tiagovportela.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A {@link FrameMetricsListener} that writes per-frame latency/throughput
 * and per-stage timing data to two CSV files.
 * <p>
 * All write methods are synchronized for thread safety.
 */
public class FpsCsvExporter implements FrameMetricsListener {

    private final String outputPath;
    private final String stageOutputPath;
    private BufferedWriter writer;
    private BufferedWriter stageWriter;

    public FpsCsvExporter(String outputPath, String stageOutputPath) {
        this.outputPath = outputPath;
        this.stageOutputPath = stageOutputPath;
    }

    @Override
    public synchronized void onStart() {
        try {
            new File(outputPath).getParentFile().mkdirs();
            writer = new BufferedWriter(new FileWriter(outputPath));
            writer.write("frame_index,latency_ms,throughput_ms,latency_fps,throughput_fps");
            writer.newLine();

            new File(stageOutputPath).getParentFile().mkdirs();
            stageWriter = new BufferedWriter(new FileWriter(stageOutputPath));
            stageWriter.write("frame_index,stage,duration_ms");
            stageWriter.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open CSV files", e);
        }
    }

    @Override
    public synchronized void onFrameProcessed(int frameIndex, long latencyNanos, long throughputNanos) {
        if (writer == null) {
            throw new IllegalStateException("onStart() must be called before onFrameProcessed()");
        }
        try {
            double latencyMs = latencyNanos / 1_000_000.0;
            double throughputMs = throughputNanos / 1_000_000.0;
            double latencyFps = latencyMs > 0 ? 1000.0 / latencyMs : 0.0;
            double throughputFps = throughputMs > 0 ? 1000.0 / throughputMs : 0.0;
            writer.write(String.format("%d,%.2f,%.2f,%.2f,%.2f",
                    frameIndex, latencyMs, throughputMs, latencyFps, throughputFps));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV row for frame " + frameIndex, e);
        }
    }

    @Override
    public synchronized void recordStage(int frameIndex, String stage, long durationNanos) {
        if (stageWriter == null) {
            throw new IllegalStateException("onStart() must be called before recordStage()");
        }
        try {
            double ms = durationNanos / 1_000_000.0;
            stageWriter.write(String.format("%d,%s,%.2f", frameIndex, stage, ms));
            stageWriter.newLine();
            stageWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write stage metric", e);
        }
    }

    @Override
    public synchronized void onFinish() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
                System.out.println("FPS metrics written to: " + outputPath);
            }
            if (stageWriter != null) {
                stageWriter.flush();
                stageWriter.close();
                System.out.println("Stage metrics written to: " + stageOutputPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to close CSV files", e);
        }
    }
}
