package io.tiagovportela.config;

public class PipelineConfig {

    private final String framesDir;
    private final String outputDir;
    private final String poseDetectionModelPath;
    private final String poseLandmarkModelPath;
    private final int detectorInputSize;
    private final int queueCapacity;
    private final int threadPoolSize;
    private final String fpsMetricsFile;
    private final String stageMetricsFile;
    private final boolean useMultithreading;

    private PipelineConfig(Builder builder) {
        this.framesDir = builder.framesDir;
        this.outputDir = builder.outputDir;
        this.poseDetectionModelPath = builder.poseDetectionModelPath;
        this.poseLandmarkModelPath = builder.poseLandmarkModelPath;
        this.detectorInputSize = builder.detectorInputSize;
        this.queueCapacity = builder.queueCapacity;
        this.threadPoolSize = builder.threadPoolSize;
        this.fpsMetricsFile = builder.fpsMetricsFile;
        this.stageMetricsFile = builder.stageMetricsFile;
        this.useMultithreading = builder.useMultithreading;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFramesDir() { return framesDir; }
    public String getOutputDir() { return outputDir; }
    public String getPoseDetectionModelPath() { return poseDetectionModelPath; }
    public String getPoseLandmarkModelPath() { return poseLandmarkModelPath; }
    public int getDetectorInputSize() { return detectorInputSize; }
    public int getQueueCapacity() { return queueCapacity; }
    public int getThreadPoolSize() { return threadPoolSize; }
    public String getFpsMetricsFile() { return fpsMetricsFile; }
    public String getStageMetricsFile() { return stageMetricsFile; }
    public boolean isUseMultithreading() { return useMultithreading; }

    public static class Builder {
        private String framesDir = "src/main/java/io/tiagovportela/videoproducer/frames";
        private String outputDir = "output";
        private String poseDetectionModelPath = "src/main/resources/models/pose_detection.tflite";
        private String poseLandmarkModelPath = "src/main/resources/models/pose_landmark_heavy.tflite";
        private int detectorInputSize = 224;
        private int queueCapacity = 2;
        private int threadPoolSize = 4;
        private String fpsMetricsFile = "results/fps_metrics.csv";
        private String stageMetricsFile = "results/pipeline_stage_metrics.csv";
        private boolean useMultithreading = true;

        public Builder framesDir(String framesDir) { this.framesDir = framesDir; return this; }
        public Builder outputDir(String outputDir) { this.outputDir = outputDir; return this; }
        public Builder poseDetectionModelPath(String path) { this.poseDetectionModelPath = path; return this; }
        public Builder poseLandmarkModelPath(String path) { this.poseLandmarkModelPath = path; return this; }
        public Builder detectorInputSize(int size) { this.detectorInputSize = size; return this; }
        public Builder queueCapacity(int capacity) { this.queueCapacity = capacity; return this; }
        public Builder threadPoolSize(int size) { this.threadPoolSize = size; return this; }
        public Builder fpsMetricsFile(String file) { this.fpsMetricsFile = file; return this; }
        public Builder stageMetricsFile(String file) { this.stageMetricsFile = file; return this; }
        public Builder useMultithreading(boolean use) { this.useMultithreading = use; return this; }

        public PipelineConfig build() {
            return new PipelineConfig(this);
        }
    }
}
