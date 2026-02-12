package Math;

import org.joml.Vector3f;

/**
 * A capsule shape: a cylinder with hemispherical caps on both ends. Useful for
 * character collision shapes.
 */
public class Capsule extends Shape {

    private static final int DEFAULT_SEGMENTS = 16;
    private static final int DEFAULT_RINGS = 8;

    private Vector3f center;
    private float radius;
    private float height; // Total height including caps
    private int segments;
    private int rings;

    private Capsule() {
        this.center = new Vector3f(0, 0, 0);
        this.radius = 0.5f;
        this.height = 2;
        this.segments = DEFAULT_SEGMENTS;
        this.rings = DEFAULT_RINGS;
    }

    public Capsule(Vector3f center, float radius, float height) {
        this(center, radius, height, DEFAULT_SEGMENTS, DEFAULT_RINGS);
    }

    public Capsule(Vector3f center, float radius, float height, int segments, int rings) {
        this.center = center;
        this.radius = radius;
        this.height = height;
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

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * Returns the height of just the cylindrical portion (excluding caps).
     */
    public float getCylinderHeight() {
        return Math.max(0, height - 2 * radius);
    }

    @Override
    public float[] getVertices() {
        float cylHeight = getCylinderHeight();
        float halfCyl = cylHeight / 2;

        // Top hemisphere + cylinder rings + bottom hemisphere
        int topCapVerts = (rings / 2 + 1) * (segments + 1);
        int cylVerts = 2 * (segments + 1);
        int bottomCapVerts = (rings / 2 + 1) * (segments + 1);
        float[] positions = new float[(topCapVerts + cylVerts + bottomCapVerts) * 3];

        int idx = 0;

        // Top hemisphere (from top pole down to equator)
        for (int r = 0; r <= rings / 2; r++) {
            float phi = (float) Math.PI * r / rings;
            float y = (float) Math.cos(phi) * radius + center.y + halfCyl;
            float ringRadius = (float) Math.sin(phi) * radius;

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                positions[idx++] = (float) Math.cos(theta) * ringRadius + center.x;
                positions[idx++] = y;
                positions[idx++] = (float) Math.sin(theta) * ringRadius + center.z;
            }
        }

        // Cylinder (top and bottom rings)
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            float x = (float) Math.cos(theta) * radius + center.x;
            float z = (float) Math.sin(theta) * radius + center.z;
            positions[idx++] = x;
            positions[idx++] = center.y + halfCyl;
            positions[idx++] = z;
        }
        for (int i = 0; i <= segments; i++) {
            float theta = 2.0f * (float) Math.PI * i / segments;
            float x = (float) Math.cos(theta) * radius + center.x;
            float z = (float) Math.sin(theta) * radius + center.z;
            positions[idx++] = x;
            positions[idx++] = center.y - halfCyl;
            positions[idx++] = z;
        }

        // Bottom hemisphere (from equator down to bottom pole)
        for (int r = rings / 2; r <= rings; r++) {
            float phi = (float) Math.PI * r / rings;
            float y = (float) Math.cos(phi) * radius + center.y - halfCyl;
            float ringRadius = (float) Math.sin(phi) * radius;

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                positions[idx++] = (float) Math.cos(theta) * ringRadius + center.x;
                positions[idx++] = y;
                positions[idx++] = (float) Math.sin(theta) * ringRadius + center.z;
            }
        }

        return positions;
    }

    @Override
    public float[] getNormals() {
        int topCapVerts = (rings / 2 + 1) * (segments + 1);
        int cylVerts = 2 * (segments + 1);
        int bottomCapVerts = (rings / 2 + 1) * (segments + 1);
        float[] normals = new float[(topCapVerts + cylVerts + bottomCapVerts) * 3];

        int idx = 0;

        // Top hemisphere normals
        for (int r = 0; r <= rings / 2; r++) {
            float phi = (float) Math.PI * r / rings;
            float ny = (float) Math.cos(phi);
            float ringRadius = (float) Math.sin(phi);

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                normals[idx++] = (float) Math.cos(theta) * ringRadius;
                normals[idx++] = ny;
                normals[idx++] = (float) Math.sin(theta) * ringRadius;
            }
        }

        // Cylinder normals (pointing outward)
        for (int ring = 0; ring < 2; ring++) {
            for (int i = 0; i <= segments; i++) {
                float theta = 2.0f * (float) Math.PI * i / segments;
                normals[idx++] = (float) Math.cos(theta);
                normals[idx++] = 0;
                normals[idx++] = (float) Math.sin(theta);
            }
        }

        // Bottom hemisphere normals
        for (int r = rings / 2; r <= rings; r++) {
            float phi = (float) Math.PI * r / rings;
            float ny = (float) Math.cos(phi);
            float ringRadius = (float) Math.sin(phi);

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                normals[idx++] = (float) Math.cos(theta) * ringRadius;
                normals[idx++] = ny;
                normals[idx++] = (float) Math.sin(theta) * ringRadius;
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
        int topCapTris = (rings / 2) * segments * 2;
        int cylTris = segments * 2;
        int bottomCapTris = (rings / 2) * segments * 2;
        int[] indices = new int[(topCapTris + cylTris + bottomCapTris) * 3];

        int idx = 0;

        // Top hemisphere triangles
        for (int r = 0; r < rings / 2; r++) {
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

        // Cylinder triangles
        int cylStart = (rings / 2 + 1) * (segments + 1);
        int cylBottomStart = cylStart + segments + 1;
        for (int i = 0; i < segments; i++) {
            int tl = cylStart + i;
            int tr = cylStart + i + 1;
            int bl = cylBottomStart + i;
            int br = cylBottomStart + i + 1;

            indices[idx++] = tl;
            indices[idx++] = bl;
            indices[idx++] = tr;

            indices[idx++] = tr;
            indices[idx++] = bl;
            indices[idx++] = br;
        }

        // Bottom hemisphere triangles
        int bottomStart = cylBottomStart + segments + 1;
        for (int r = 0; r < rings / 2; r++) {
            for (int s = 0; s < segments; s++) {
                int current = bottomStart + r * (segments + 1) + s;
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
