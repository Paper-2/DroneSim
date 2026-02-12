package com.paperpiper.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Holds a mesh along with its local transformation matrix and material color
 * within a model. This preserves the relative position/rotation/scale of the
 * mesh as defined in the model file.
 */
public class MeshData {

    private final Mesh mesh; // Reference to the shared Mesh object (geometry)
    private final Matrix4f localTransform; // Position/rotation/scale of this mesh relative to the model's origin
    private final Vector3f color;

    public MeshData(Mesh mesh, Matrix4f localTransform) {
        this(mesh, localTransform, new Vector3f(0.8f, 0.8f, 0.8f));
    }

    public MeshData(Mesh mesh, Matrix4f localTransform, Vector3f color) {
        this.mesh = mesh;
        this.localTransform = localTransform != null ? new Matrix4f(localTransform) : new Matrix4f().identity();
        this.color = color != null ? new Vector3f(color) : new Vector3f(0.8f, 0.8f, 0.8f);
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Matrix4f getLocalTransform() {
        return localTransform;
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(float r, float g, float b) {
        color.set(r, g, b);
    }

    public void setColor(Vector3f color) {
        this.color.set(color);
    }
}
