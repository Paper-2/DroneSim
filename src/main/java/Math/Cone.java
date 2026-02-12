package Math;

import org.joml.Vector3f;

public class Cone extends Shape {

    private static final int DEFAULT_SEGMENTS = 32;

    private Vector3f base;  // Center of the base
    private float radius;
    private float height;
    private int segments;

    private Cone() {
        this.base = new Vector3f(0, 0, 0);
        this.radius = 1;
        this.height = 1;
        this.segments = DEFAULT_SEGMENTS;
    }

    public Cone(Vector3f base, float radius, float height) {
        this(base, radius, height, DEFAULT_SEGMENTS);
    }

    public Cone(Vector3f base, float radius, float height, int segments) {
        this.base = base;
        this.radius = radius;
        this.height = height;
        this.segments = segments;
    }

    public Vector3f getBase() {
        return base;
    }

    public void setBase(Vector3f base) {
        this.base = base;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * Returns the apex (tip) of the cone.
     */
    public Vector3f getApex() {
        return new Vector3f(base.x, base.y + height, base.z);
    }

    @Override
    public float[] getVertices() {
        // Apex + base edge vertices (for sides) + base center + base edge vertices (for cap)
        int sideVerts = segments + 2; // apex + edge vertices + wrap
        int capVerts = segments + 2;  // center + edge + wrap
        float[] positions = new float[(sideVerts + capVerts) * 3];

        int idx = 0;

        // Apex vertex
        positions[idx++] = base.x;
        positions[idx++] = base.y + height;
        positions[idx++] = base.z;

        // Side base edge vertices
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            positions[idx++] = (float) Math.cos(theta) * radius + base.x;
            positions[idx++] = base.y;
            positions[idx++] = (float) Math.sin(theta) * radius + base.z;
        }

        // Base cap center
        positions[idx++] = base.x;
        positions[idx++] = base.y;
        positions[idx++] = base.z;

        // Base cap edge vertices
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            positions[idx++] = (float) Math.cos(theta) * radius + base.x;
            positions[idx++] = base.y;
            positions[idx++] = (float) Math.sin(theta) * radius + base.z;
        }

        return positions;
    }

    @Override
    public float[] getNormals() {
        int sideVerts = segments + 2;
        int capVerts = segments + 2;
        float[] normals = new float[(sideVerts + capVerts) * 3];

        // Slope angle for cone side normals
        float slope = radius / height;
        float normalY = slope / (float) Math.sqrt(1 + slope * slope);
        float normalScale = 1 / (float) Math.sqrt(1 + slope * slope);

        int idx = 0;

        // Apex normal (average of surrounding normals, pointing up-ish)
        normals[idx++] = 0;
        normals[idx++] = normalY;
        normals[idx++] = 0;

        // Side normals
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            normals[idx++] = (float) Math.cos(theta) * normalScale;
            normals[idx++] = normalY;
            normals[idx++] = (float) Math.sin(theta) * normalScale;
        }

        // Base cap normals (pointing down)
        for (int i = 0; i <= segments + 1; i++) {
            normals[idx++] = 0;
            normals[idx++] = -1;
            normals[idx++] = 0;
        }

        return normals;
    }

    @Override
    public float[] getTexCoords() {
        return null;
    }

    @Override
    public int[] getFaces() {
        int sideTriangles = segments;
        int capTriangles = segments;
        int[] indices = new int[(sideTriangles + capTriangles) * 3];

        int idx = 0;
        int apex = 0;
        int baseEdgeStart = 1;

        // Side triangles
        for (int i = 0; i < segments; i++) {
            indices[idx++] = apex;
            indices[idx++] = baseEdgeStart + i + 1;
            indices[idx++] = baseEdgeStart + i;
        }

        // Base cap
        int capCenter = segments + 2;
        int capEdgeStart = capCenter + 1;
        for (int i = 0; i < segments; i++) {
            indices[idx++] = capCenter;
            indices[idx++] = capEdgeStart + i + 1;
            indices[idx++] = capEdgeStart + i;
        }

        return indices;
    }
}
