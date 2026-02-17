package io.tiagovportela.visualization;

import io.tiagovportela.datatypes.BoundingBox;
import io.tiagovportela.datatypes.Landmark;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

/**
 * Draws pose landmarks, skeleton connections, and bounding boxes onto
 * OpenCV frames. Supports saving annotated frames to disk.
 *
 * <p>
 * Uses the 33-landmark BlazePose topology with configurable colours
 * and line thickness.
 * </p>
 */
public class PoseVisualizer {

    /* ---- BlazePose skeleton connections (pairs of landmark indices) ---- */
    private static final int[][] SKELETON = {
            // Face
            { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 7 },
            { 0, 4 }, { 4, 5 }, { 5, 6 }, { 6, 8 },
            { 9, 10 },
            // Torso
            { 11, 12 }, { 11, 23 }, { 12, 24 }, { 23, 24 },
            // Left arm
            { 11, 13 }, { 13, 15 }, { 15, 17 }, { 15, 19 }, { 15, 21 }, { 17, 19 },
            // Right arm
            { 12, 14 }, { 14, 16 }, { 16, 18 }, { 16, 20 }, { 16, 22 }, { 18, 20 },
            // Left leg
            { 23, 25 }, { 25, 27 }, { 27, 29 }, { 27, 31 }, { 29, 31 },
            // Right leg
            { 24, 26 }, { 26, 28 }, { 28, 30 }, { 28, 32 }, { 30, 32 }
    };

    /* ---- default drawing style ---- */
    private static final Scalar LANDMARK_COLOR = new Scalar(0, 255, 0); // green
    private static final Scalar SKELETON_COLOR = new Scalar(255, 255, 0); // cyan
    private static final Scalar BBOX_COLOR = new Scalar(0, 0, 255); // red
    private static final int LANDMARK_RADIUS = 4;
    private static final int LINE_THICKNESS = 2;
    private static final float MIN_VISIBILITY = 0.5f;

    private final String outputDir;

    /**
     * Creates a visualizer that saves annotated frames to the given directory.
     *
     * @param outputDir directory path where annotated images will be saved
     */
    public PoseVisualizer(String outputDir) {
        this.outputDir = outputDir;
        new java.io.File(outputDir).mkdirs();
    }

    /**
     * Draws landmarks, skeleton, and bounding box onto a copy of the frame
     * and saves the result as a PNG image.
     *
     * @param frame       the original BGR frame
     * @param landmarks   the 33 pose landmarks (may be {@code null})
     * @param boundingBox the detected bounding box (may be {@code null})
     * @param frameIndex  used to name the output file
     */
    public void draw(Mat frame, Landmark[] landmarks, BoundingBox boundingBox, int frameIndex) {
        Mat canvas = frame.clone();

        if (boundingBox != null) {
            drawBoundingBox(canvas, boundingBox);
        }

        if (landmarks != null) {
            drawSkeleton(canvas, landmarks);
            drawLandmarks(canvas, landmarks);
        }

        String filename = String.format("%s/frame_%04d.png", outputDir, frameIndex);
        Imgcodecs.imwrite(filename, canvas);
        canvas.release();
    }

    /**
     * Draws landmarks and skeleton onto the frame in-place (no file save).
     *
     * @param frame     the BGR frame to annotate (modified in-place)
     * @param landmarks the 33 pose landmarks
     */
    public void drawOnFrame(Mat frame, Landmark[] landmarks) {
        if (landmarks != null) {
            drawSkeleton(frame, landmarks);
            drawLandmarks(frame, landmarks);
        }
    }

    /*
     * ================================================================
     * Private drawing helpers
     * ================================================================
     */

    private void drawBoundingBox(Mat frame, BoundingBox box) {
        int[] abs = box.toAbsolute(frame.cols(), frame.rows());
        Imgproc.rectangle(frame,
                new Point(abs[0], abs[1]),
                new Point(abs[0] + abs[2], abs[1] + abs[3]),
                BBOX_COLOR, LINE_THICKNESS);
    }

    private void drawLandmarks(Mat frame, Landmark[] landmarks) {
        int w = frame.cols();
        int h = frame.rows();

        for (Landmark lm : landmarks) {
            if (lm.getVisibility() < MIN_VISIBILITY)
                continue;

            int px = Math.round(lm.getX() * w);
            int py = Math.round(lm.getY() * h);
            Imgproc.circle(frame, new Point(px, py), LANDMARK_RADIUS, LANDMARK_COLOR, -1);
        }
    }

    private void drawSkeleton(Mat frame, Landmark[] landmarks) {
        int w = frame.cols();
        int h = frame.rows();

        for (int[] edge : SKELETON) {
            Landmark a = landmarks[edge[0]];
            Landmark b = landmarks[edge[1]];

            if (a.getVisibility() < MIN_VISIBILITY || b.getVisibility() < MIN_VISIBILITY) {
                continue;
            }

            Point p1 = new Point(Math.round(a.getX() * w), Math.round(a.getY() * h));
            Point p2 = new Point(Math.round(b.getX() * w), Math.round(b.getY() * h));
            Imgproc.line(frame, p1, p2, SKELETON_COLOR, LINE_THICKNESS);
        }
    }
}
