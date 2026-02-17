package io.tiagovportela.metrics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A {@link FrameMetricsListener} that writes per-frame FPS data to a CSV file.
 *
 * <p>
 * CSV columns: {@code frame_index, elapsed_ms, fps}
 * </p>
 */
public class FpsCsvExporter implements FrameMetricsListener {

    private final String outputPath;
    private BufferedWriter writer;

    /**
     * Creates an exporter that will write metrics to the given file path.
     *
     * @param outputPath path to the output CSV file
     */
    public FpsCsvExporter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void onStart() {
        try {
            writer = new BufferedWriter(new FileWriter(outputPath));
            writer.write("frame_index,elapsed_ms,fps");
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open CSV file: " + outputPath, e);
        }
    }

    @Override
    public void onFrameProcessed(int frameIndex, long elapsedNanos) {
        if (writer == null) {
            throw new IllegalStateException("onStart() must be called before onFrameProcessed()");
        }
        try {
            double elapsedMs = elapsedNanos / 1_000_000.0;
            double fps = elapsedMs > 0 ? 1000.0 / elapsedMs : 0.0;
            writer.write(String.format("%d,%.2f,%.2f", frameIndex, elapsedMs, fps));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write CSV row for frame " + frameIndex, e);
        }
    }

    @Override
    public void onFinish() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                System.out.println("FPS metrics written to: " + outputPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to close CSV file: " + outputPath, e);
            }
        }
    }
}
