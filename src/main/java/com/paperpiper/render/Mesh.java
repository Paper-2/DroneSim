package com.paperpiper.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
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

/**
 * Mesh class for storing and rendering 3D geometry.
 */
public class Mesh {

    private int vaoId;
    private int posVboId;
    private int normalVboId;
    private int idxVboId;
    private int vertexCount;

    // messh constructor
    public Mesh(float[] positions, float[] normals, int[] indices) {
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
            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }

    // Vertex Array Object ID
    public int getVaoId() {
        return vaoId;
    }

    // Vertex count
    public int getVertexCount() {
        return vertexCount;
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(posVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(idxVboId);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }


    // Loads a cube mesh
    public static Mesh createCube(float size) {
        float s = size / 2;

        float[] positions = {
            -s, -s, s, s, -s, s, s, s, s, -s, s, s,// Front face
            s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s,// Back face
            -s, s, s, s, s, s, s, s, -s, -s, s, -s,// Top face
            -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s,// Bottom face
            s, -s, s, s, -s, -s, s, s, -s, s, s, s,// Right face
            -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s// Left face
        };

        float[] normals = {
            // Front
            0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1,
            // Back
            0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1,
            // Top
            0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0,
            // Bottom
            0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0,
            // Right
            1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0,
            // Left
            -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0, // Front
            4, 5, 6, 6, 7, 4, // Back
            8, 9, 10, 10, 11, 8, // Top
            12, 13, 14, 14, 15, 12, // Bottom
            16, 17, 18, 18, 19, 16, // Right
            20, 21, 22, 22, 23, 20 // Left
        };

        return new Mesh(positions, normals, indices);
    }


    // Loads a plane mesh
    public static Mesh createPlane(float width, float depth) {
        float w = width / 2;
        float d = depth / 2;

        float[] positions = {
            -w, 0, -d,
            w, 0, -d,
            w, 0, d,
            -w, 0, d
        };

        float[] normals = {
            0, 1, 0,
            0, 1, 0,
            0, 1, 0,
            0, 1, 0
        };

        int[] indices = {
            0, 2, 1,
            0, 3, 2
        };

        return new Mesh(positions, normals, indices);
    }
}
