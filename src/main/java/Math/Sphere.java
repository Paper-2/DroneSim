package Math;

import org.joml.Vector3f;

public class Sphere extends Shape {

    private static final int DEFAULT_SEGMENTS = 32;
    private static final int DEFAULT_RINGS = 16;

    private Vector3f center;
    private float radius;
    private int segments;
    private int rings;

    private Sphere() {
        this.center = new Vector3f(0, 0, 0);
        this.radius = 1;
        this.segments = DEFAULT_SEGMENTS;
        this.rings = DEFAULT_RINGS;
    }

    public Sphere(Vector3f center, float radius) {
        this(center, radius, DEFAULT_SEGMENTS, DEFAULT_RINGS);
    }

    public Sphere(Vector3f center, float radius, int segments, int rings) {
        this.center = center;
        this.radius = radius;
        this.segments = segments;
        this.rings = rings;
    }

    public Vector3f getCenter() {
        return center;
    }

    public void setCenter(Vector3f center) {
        this.center = center;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public float[] getVertices() {
        int vertexCount = (rings + 1) * (segments + 1);
        float[] positions = new float[vertexCount * 3];

        int idx = 0;
        for (int r = 0; r <= rings; r++) {
            float phi = (float) Math.PI * r / rings;
            float y = (float) Math.cos(phi) * radius + center.y;
            float ringRadius = (float) Math.sin(phi) * radius;

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                float x = (float) Math.cos(theta) * ringRadius + center.x;
                float z = (float) Math.sin(theta) * ringRadius + center.z;

                positions[idx++] = x;
                positions[idx++] = y;
                positions[idx++] = z;
            }
        }
        return positions;
    }

    @Override
    public float[] getNormals() {
        int vertexCount = (rings + 1) * (segments + 1);
        float[] normals = new float[vertexCount * 3];

        int idx = 0;
        for (int r = 0; r <= rings; r++) {
            float phi = (float) Math.PI * r / rings;
            float ny = (float) Math.cos(phi);
            float ringRadius = (float) Math.sin(phi);

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                float nx = (float) Math.cos(theta) * ringRadius;
                float nz = (float) Math.sin(theta) * ringRadius;

                normals[idx++] = nx;
                normals[idx++] = ny;
                normals[idx++] = nz;
            }
        }
        return normals;
    }

    @Override
    public float[] getTexCoords() {
        return null;
    }

    @Override
    public int[] getFaces() {
        int indexCount = rings * segments * 6;
        int[] indices = new int[indexCount];
        int idx = 0;

        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < segments; s++) {
                int current = r * (segments + 1) + s;
                int next = current + segments + 1;

                indices[idx++] = current;
                indices[idx++] = next;
                indices[idx++] = current + 1;

                indices[idx++] = current + 1;
                indices[idx++] = next;
                indices[idx++] = next + 1;
            }
        }
        return indices;
    }
}
