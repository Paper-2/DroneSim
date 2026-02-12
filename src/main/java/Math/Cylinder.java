package Math;

import org.joml.Vector3f;

public class Cylinder extends Shape {

    private static final int DEFAULT_SEGMENTS = 32;

    private Vector3f center;
    private float radius;
    private float height;
    private int segments;

    private Cylinder() {
        this.center = new Vector3f(0, 0, 0);
        this.radius = 1;
        this.height = 1;
        this.segments = DEFAULT_SEGMENTS;
    }

    public Cylinder(Vector3f center, float radius, float height) {
        this(center, radius, height, DEFAULT_SEGMENTS);
    }

    public Cylinder(Vector3f center, float radius, float height, int segments) {
        this.center = center;
        this.radius = radius;
        this.height = height;
        this.segments = segments;
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

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    @Override
    public float[] getVertices() {
        // Side vertices + top cap center + bottom cap center + cap edge vertices
        int sideVerts = (segments + 1) * 2;
        int capVerts = (segments + 1) * 2 + 2; // edges + centers
        float[] positions = new float[(sideVerts + capVerts) * 3];

        float halfH = height / 2;
        int idx = 0;

        // Side vertices (bottom ring then top ring)
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            float x = (float) Math.cos(theta) * radius + center.x;
            float z = (float) Math.sin(theta) * radius + center.z;

            positions[idx++] = x;
            positions[idx++] = center.y - halfH;
            positions[idx++] = z;
        }
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            float x = (float) Math.cos(theta) * radius + center.x;
            float z = (float) Math.sin(theta) * radius + center.z;

            positions[idx++] = x;
            positions[idx++] = center.y + halfH;
            positions[idx++] = z;
        }

        // Top cap center
        positions[idx++] = center.x;
        positions[idx++] = center.y + halfH;
        positions[idx++] = center.z;

        // Top cap edge vertices
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            positions[idx++] = (float) Math.cos(theta) * radius + center.x;
            positions[idx++] = center.y + halfH;
            positions[idx++] = (float) Math.sin(theta) * radius + center.z;
        }

        // Bottom cap center
        positions[idx++] = center.x;
        positions[idx++] = center.y - halfH;
        positions[idx++] = center.z;

        // Bottom cap edge vertices
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            positions[idx++] = (float) Math.cos(theta) * radius + center.x;
            positions[idx++] = center.y - halfH;
            positions[idx++] = (float) Math.sin(theta) * radius + center.z;
        }

        return positions;
    }

    @Override
    public float[] getNormals() {
        int sideVerts = (segments + 1) * 2;
        int capVerts = (segments + 1) * 2 + 2;
        float[] normals = new float[(sideVerts + capVerts) * 3];

        int idx = 0;

        // Side normals (pointing outward)
        for (int ring = 0; ring < 2; ring++) {
            for (int i = 0; i <= segments; i++) {
                float theta = 2.0f * (float) Math.PI * i / segments;
                normals[idx++] = (float) Math.cos(theta);
                normals[idx++] = 0;
                normals[idx++] = (float) Math.sin(theta);
            }
        }

        // Top cap normals (pointing up)
        for (int i = 0; i <= segments + 1; i++) {
            normals[idx++] = 0;
            normals[idx++] = 1;
            normals[idx++] = 0;
        }

        // Bottom cap normals (pointing down)
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
        int sideTriangles = segments * 2;
        int capTriangles = segments * 2;
        int[] indices = new int[(sideTriangles + capTriangles) * 3];

        int idx = 0;
        int topRingStart = segments + 1;

        // Side faces
        for (int i = 0; i < segments; i++) {
            int bl = i;
            int br = i + 1;
            int tl = topRingStart + i;
            int tr = topRingStart + i + 1;

            indices[idx++] = bl;
            indices[idx++] = br;
            indices[idx++] = tr;

            indices[idx++] = bl;
            indices[idx++] = tr;
            indices[idx++] = tl;
        }

        // Top cap
        int topCenter = (segments + 1) * 2;
        int topEdgeStart = topCenter + 1;
        for (int i = 0; i < segments; i++) {
            indices[idx++] = topCenter;
            indices[idx++] = topEdgeStart + i;
            indices[idx++] = topEdgeStart + i + 1;
        }

        // Bottom cap
        int bottomCenter = topEdgeStart + segments + 1;
        int bottomEdgeStart = bottomCenter + 1;
        for (int i = 0; i < segments; i++) {
            indices[idx++] = bottomCenter;
            indices[idx++] = bottomEdgeStart + i + 1;
            indices[idx++] = bottomEdgeStart + i;
        }

        return indices;
    }
}
