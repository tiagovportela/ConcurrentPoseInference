package io.tiagovportela.datatypes;

/**
 * Stores a detected bounding box with normalised coordinates (0–1 range)
 * relative to the original frame dimensions.
 */
public class BoundingBox {

    private final float xMin;
    private final float yMin;
    private final float width;
    private final float height;
    private final float score;

    /**
     * @param xMin   left edge (normalised 0–1)
     * @param yMin   top edge (normalised 0–1)
     * @param width  box width (normalised 0–1)
     * @param height box height (normalised 0–1)
     * @param score  detection confidence (0–1)
     */
    public BoundingBox(float xMin, float yMin, float width, float height, float score) {
        this.xMin = xMin;
        this.yMin = yMin;
        this.width = width;
        this.height = height;
        this.score = score;
    }

    public float getXMin() {
        return xMin;
    }

    public float getYMin() {
        return yMin;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getScore() {
        return score;
    }

    /**
     * Returns the absolute bounding box coordinates for a given frame size.
     *
     * @param frameWidth  width of the original frame in pixels
     * @param frameHeight height of the original frame in pixels
     * @return int array {x, y, width, height} in pixel coordinates
     */
    public int[] toAbsolute(int frameWidth, int frameHeight) {
        return new int[] {
                Math.round(xMin * frameWidth),
                Math.round(yMin * frameHeight),
                Math.round(width * frameWidth),
                Math.round(height * frameHeight)
        };
    }

    @Override
    public String toString() {
        return String.format("BoundingBox[x=%.3f, y=%.3f, w=%.3f, h=%.3f, score=%.3f]",
                xMin, yMin, width, height, score);
    }
}
