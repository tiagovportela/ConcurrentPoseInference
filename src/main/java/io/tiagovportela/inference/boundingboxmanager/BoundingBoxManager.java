package io.tiagovportela.inference.boundingboxmanager;

import io.tiagovportela.datatypes.BoundingBox;

public class BoundingBoxManager {
        // This class is responsible for managing the bounding box data across the pipeline.
        // It can store the latest bounding box, provide thread-safe access to it, and handle any necessary transformations.
        // For simplicity, we will just store the latest bounding box and provide synchronized access to it.

        private volatile BoundingBox latestBoundingBox;
        private long lastUpdateTime;
        private float lastLandmarksAvgScore;
        private long time_threshold = 100_000_000; // 100 ms in nanoseconds
        private float landmarksAvgScoreThreshold = 0.5f;

        public synchronized void updateBoundingBox(BoundingBox box) {
            this.latestBoundingBox = box;
            this.lastUpdateTime = System.nanoTime();
        }

        public synchronized void updateLandmarksAvgScore(float score) {
            this.lastLandmarksAvgScore = score;
        }


        public synchronized float getLandmarksAvgScore() {
            return this.lastLandmarksAvgScore;
        }

        public synchronized BoundingBox getLatestBoundingBox() {
            return this.latestBoundingBox;
        }

        public boolean isToUpdateBoundingBox(long currentTime) {
            return (currentTime - lastUpdateTime) > time_threshold || getLandmarksAvgScore() < landmarksAvgScoreThreshold || getLatestBoundingBox() == null;
        }


}
