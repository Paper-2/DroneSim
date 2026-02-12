package Math;

import org.joml.Vector3f;

public class Plane extends Shape {

    private Vector3f point;
    private Vector3f normal;
    private float size;

    private Plane() {
        this.point = new Vector3f(0, 0, 0);
        this.normal = new Vector3f(0, 1, 0);
        this.size = 10;
    }

    public Plane(Vector3f point, Vector3f normal) {
        this(point, normal, 10);
    }

    public Plane(Vector3f point, Vector3f normal, float size) {
        this.point = point;
        this.normal = new Vector3f(normal).normalize();
        this.size = size;
    }

    /**
     * Creates a horizontal plane at the given height.
     */
    public static Plane horizontal(float height) {
        return new Plane(new Vector3f(0, height, 0), new Vector3f(0, 1, 0));
    }

    public Vector3f getPoint() {
        return point;
    }

    public void setPoint(Vector3f point) {
        this.point = point;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public void setNormal(Vector3f normal) {
        this.normal = new Vector3f(normal).normalize();
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    /**
     * Returns the signed distance from a point to this plane. Positive = same
     * side as normal, Negative = opposite side.
     */
    public float signedDistance(Vector3f p) {
        return normal.dot(new Vector3f(p).sub(point));
    }

    @Override
    public float[] getVertices() {
        // Compute tangent and bitangent vectors
        Vector3f tangent = new Vector3f();
        if (Math.abs(normal.y) < 0.9f) {
            tangent.set(0, 1, 0).cross(normal).normalize();
        } else {
            tangent.set(1, 0, 0).cross(normal).normalize();
        }
        Vector3f bitangent = new Vector3f(normal).cross(tangent).normalize();

        float hs = size / 2;
        return new float[]{
            point.x - tangent.x * hs - bitangent.x * hs,
            point.y - tangent.y * hs - bitangent.y * hs,
            point.z - tangent.z * hs - bitangent.z * hs,
            point.x + tangent.x * hs - bitangent.x * hs,
            point.y + tangent.y * hs - bitangent.y * hs,
            point.z + tangent.z * hs - bitangent.z * hs,
            point.x + tangent.x * hs + bitangent.x * hs,
            point.y + tangent.y * hs + bitangent.y * hs,
            point.z + tangent.z * hs + bitangent.z * hs,
            point.x - tangent.x * hs + bitangent.x * hs,
            point.y - tangent.y * hs + bitangent.y * hs,
            point.z - tangent.z * hs + bitangent.z * hs
        };
    }

    @Override
    public float[] getNormals() {
        return new float[]{
            normal.x, normal.y, normal.z,
            normal.x, normal.y, normal.z,
            normal.x, normal.y, normal.z,
            normal.x, normal.y, normal.z
        };
    }

    @Override
    public float[] getTexCoords() {
        return null;
    }

    @Override
    public int[] getFaces() {
        return new int[]{0, 1, 2, 2, 3, 0};
    }
}
