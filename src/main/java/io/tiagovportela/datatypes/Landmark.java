package io.tiagovportela.datatypes;

/**
 * Represents a single pose landmark with 3D coordinates and confidence scores.
 * Coordinates are normalised (0–1) relative to the original frame dimensions.
 */
public class Landmark {

    private final float x;
    private final float y;
    private final float z;
    private final float visibility;
    private final float presence;

    /**
     * @param x          horizontal position (normalised 0–1)
     * @param y          vertical position (normalised 0–1)
     * @param z          depth relative to hip midpoint
     * @param visibility how visible this landmark is (0–1)
     * @param presence   likelihood this landmark exists (0–1)
     */
    public Landmark(float x, float y, float z, float visibility, float presence) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.visibility = visibility;
        this.presence = presence;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getVisibility() {
        return visibility;
    }

    public float getPresence() {
        return presence;
    }

    @Override
    public String toString() {
        return String.format("Landmark[x=%.3f, y=%.3f, z=%.3f, vis=%.3f, pres=%.3f]",
                x, y, z, visibility, presence);
    }
}
