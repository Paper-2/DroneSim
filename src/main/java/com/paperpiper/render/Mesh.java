package com.paperpiper.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_FLOAT; // the java package manager expands .*; I hate it.
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import org.lwjgl.system.MemoryUtil;

import com.jme3.math.Vector3f;

import Math.*;
import Math.Plane;
import Math.Shape;
import Math.Sphere;

public class Mesh {

    private int vaoId;
    private int posVboId;
    private int normalVboId;
    private int texCoordVboId;
    private int idxVboId;
    private int vertexCount;
    private int textureId;
    private final String meshName;
    private String displayName; // Can be modified to add prefixes, etc

    // Store original vertex data for cloning
    private final float[] positions; // vertex positions
    private final float[] normals;
    private final float[] texCoords; // texture coordinates
    private final int[] indices;     // connections between vertices

    @SuppressWarnings("unused")
    private Map<String, String> properties;

    // mesh constructor with UV coordinates
    public Mesh(float[] positions, float[] normals, float[] texCoords, int[] indices, String name) {
        this.meshName = name;
        this.displayName = name;

        // Store copies of the original vertex data for cloning
        this.positions = positions.clone();
        this.normals = normals.clone();
        this.texCoords = texCoords.clone();
        this.indices = indices.clone();

        FloatBuffer texCoordBuffer = null;
        initializeGraphicsBuffers(positions, normals, texCoords, indices, texCoordBuffer);
    }

    private void initializeGraphicsBuffers(float[] positions, float[] normals, float[] texCoords, int[] indices,
            FloatBuffer texCoordBuffer) {
        FloatBuffer posBuffer = null;
        FloatBuffer normalBuffer = null;
        IntBuffer indicesBuffer = null;
        try {
            vertexCount = indices.length;

            // Create VAO
            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Position VBO
            posVboId = glGenBuffers();
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, posVboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Normal VBO
            normalVboId = glGenBuffers();
            normalBuffer = MemoryUtil.memAllocFloat(normals.length);
            normalBuffer.put(normals).flip();
            glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
            glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);

            // Texture Coordinate VBO
            if (texCoords != null && texCoords.length > 0) {
                texCoordVboId = glGenBuffers();
                texCoordBuffer = MemoryUtil.memAllocFloat(texCoords.length);
                texCoordBuffer.put(texCoords).flip();
                glBindBuffer(GL_ARRAY_BUFFER, texCoordVboId);
                glBufferData(GL_ARRAY_BUFFER, texCoordBuffer, GL_STATIC_DRAW);
                glEnableVertexAttribArray(2);
                glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
            }

            // Index VBO
            idxVboId = glGenBuffers();
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, idxVboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            // Unbind
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

        } finally {
            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer);
            }
            if (normalBuffer != null) {
                MemoryUtil.memFree(normalBuffer);
            }
            if (texCoordBuffer != null) {
                MemoryUtil.memFree(texCoordBuffer);
            }
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }

    public Mesh(Shape shape, String name) {

        this.meshName = name;
        this.displayName = name;

        this.positions = shape.getVertices();
        this.normals = shape.getNormals();
        this.texCoords = shape.getTexCoords(); // returns null
        this.indices = shape.getFaces();

        initializeGraphicsBuffers(positions, normals, texCoords, indices, null);
    }

    // Vertex Array Object ID
    public int getVaoId() {
        return vaoId;
    }

    // Vertex count 
    public int getVertexCount() {
        return vertexCount;
    }

    public void setTextureId(int textureId) {
        this.textureId = textureId;
    }

    public int getTexture() {
        return textureId;
    }


    /*
    * Render the mesh.
    * No need to worry about this function for now.
     */
    public void render() {
        if (textureId != 0) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }

        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        if (textureId != 0) {
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(normalVboId);
        if (texCoordVboId != 0) {
            glDeleteBuffers(texCoordVboId);
        }
        glDeleteBuffers(idxVboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    public static Mesh createSphere(float radius, int segments, int rings) {
        return new Mesh(new Sphere(new org.joml.Vector3f(0, 0, 0), radius, segments, rings), "sphere");
    }

    public static Mesh createCube(float size) {
        return createBox(size, size, size);
    }

    public static Mesh createBox(float width, float height, float depth) {
        return new Mesh(new Box(new org.joml.Vector3f(0, 0, 0), width, height, depth), "box");
    }

    public static Mesh createPlane(float width, float depth) {
        return new Mesh(new Plane(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 1, 0), Math.max(width, depth)), "plane");
    }

    public String getMeshName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Create a deep clone of this mesh with a new name. Recreates GPU resources
     * and vertex data.
     */
    public Mesh clone(String newName) {
        return new Mesh(positions, normals, texCoords, indices, newName);
    }

    public Vector3f getPosition() {
        Vector3f center = new Vector3f();

        for (int i = 0; i < this.positions.length; i += 3) {
            center.add(this.positions[i], this.positions[i + 1], this.positions[i + 2]);
        }

        // Calculate the average position to find the center of the mesh
        center = center.divideLocal(this.positions.length / 3);

        return center;
    }

    public float[] getPositions() {
        return positions;
    }
}
