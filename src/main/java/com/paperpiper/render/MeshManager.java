package com.paperpiper.render;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton manager for creating, caching, and disposing of Mesh objects.
 * Prevents duplicate meshes and centralizes lifecycle management.
 */
public class MeshManager {

    private static MeshManager instance;
    private final Map<String, Mesh> meshCache; // name → Mesh

    private MeshManager() {
        meshCache = new HashMap<>();
    }

    public static MeshManager getInstance() {
        if (instance == null) {
            instance = new MeshManager();
        }
        return instance;
    }

    /** Create a mesh from raw vertex data and cache it by name. */
    public Mesh createMesh(float[] positions, float[] normals, int[] indices, String name) {
        float[] texCoords = new float[positions.length / 3 * 2]; // Empty tex coords
        Mesh mesh = new Mesh(positions, normals, texCoords, indices, name);
        meshCache.put(name, mesh);
        return mesh;
    }

    /** Built-in primitive generators (cached automatically). */
    public Mesh getCube(float size) {
        String key = "cube_" + size;
        if (meshCache.containsKey(key)) {
            return meshCache.get(key);
        }
        Mesh cube = Mesh.createCube(size);
        meshCache.put(key, cube);
        return cube;
    }

    public Mesh getPlane(float width, float depth) {
        String key = "plane_" + width + "x" + depth;
        if (meshCache.containsKey(key)) {
            return meshCache.get(key);
        }
        Mesh plane = Mesh.createPlane(width, depth);
        meshCache.put(key, plane);
        return plane;
    }


    /** Load a model from a GLTF/GLB file, cache all its meshes, and return the Model. */
    public Model loadModel(String name, String filePath) {
        Model model = new Model(name);
        model.loadModel(filePath);
        return model;
    }


    /** Retrieve a previously cached mesh by name. */
    public Mesh getMesh(String name) {
        return meshCache.get(name);
    }

    /** Check if a mesh with the given name exists in the cache. */
    public boolean hasMesh(String name) {
        return meshCache.containsKey(name);
    }


    /** Remove and cleanup a single mesh by name. */
    public void removeMesh(String name) {
        Mesh mesh = meshCache.remove(name);
        if (mesh != null) {
            mesh.cleanup();
        }
    }

    /** Cleanup all cached meshes (call on shutdown). */
    public void cleanup() {
        for (Mesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
    }
}
