package Math;

/**
 * Abstract base class for geometric shapes that can be converted to meshes.
 */
public abstract class Shape {

    /**
     * Returns the vertex positions as a flat array [x1, y1, z1, x2, y2, z2,
     * ...].
     */
    public abstract float[] getVertices();

    /**
     * Returns the vertex normals as a flat array [nx1, ny1, nz1, nx2, ny2, nz2,
     * ...].
     */
    public abstract float[] getNormals();

    /**
     * Returns the texture coordinates as a flat array [u1, v1, u2, v2, ...].
     * May return null if no texture coordinates are needed.
     */
    public abstract float[] getTexCoords();

    /**
     * Returns the face indices as a flat array of triangles [i1, i2, i3, i4,
     * i5, i6, ...].
     */
    public abstract int[] getFaces();
}
