package com.paperpiper.render;

// TODO: Model currently uses jme3 Vector3f for public API to match physics code.
//

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f; // evil
import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import org.lwjgl.stb.STBImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a 3D model loaded from a model file using Assimp. Supports
 * multiple formats: GLB (binary), GLTF (JSON), and Blender (.blend) files.
 * Contains one or more MeshData entries, each with its own local transform,
 * plus material information extracted from the file.
 */
public class Model {

    private static final Logger logger = LoggerFactory.getLogger(Model.class);

    private final String name;
    private final List<MeshData> meshDataList;
    private final List<MaterialInfo> materials;
    private SceneNode rootNode;
    private final Map<String, List<String>> meshGroups; // group name -> list of mesh names

    public Model(String name) {
        this.name = name;
        this.meshDataList = new ArrayList<>();
        this.materials = new ArrayList<>();
        this.meshGroups = new HashMap<>();
    }

    /**
     * Load a 3D model from the given file path using Assimp. Supports multiple
     * formats: GLB (binary), GLTF (JSON), and Blender (.blend) files
     */
    public void loadModel(String filePath) {
        logger.info("Loading model '{}' from: {}", name, filePath);

        // Determine if this is a blend file for special handling
        boolean isBlendFile = filePath.toLowerCase().endsWith(".blend");

        int flags = Assimp.aiProcess_Triangulate
                | Assimp.aiProcess_GenSmoothNormals
                | Assimp.aiProcess_FlipUVs
                | Assimp.aiProcess_JoinIdenticalVertices
                | Assimp.aiProcess_CalcTangentSpace;

        // Add extra flags for blend file stability
        if (isBlendFile) {
            flags |= Assimp.aiProcess_RemoveRedundantMaterials
                    | Assimp.aiProcess_PreTransformVertices;
            logger.info("Loading Blender file with enhanced flags");
        }

        AIScene scene = Assimp.aiImportFile(filePath, flags);

        if (scene == null
                || (scene.mFlags() & Assimp.AI_SCENE_FLAGS_INCOMPLETE) != 0
                || scene.mRootNode() == null) {
            String error = Assimp.aiGetErrorString();
            throw new RuntimeException("Failed to load model '" + filePath + "': " + error);
        }

        // Load materials
        int numMaterials = scene.mNumMaterials();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(scene.mMaterials().get(i));
            materials.add(processMaterial(aiMaterial));
        }
        logger.info("Loaded {} material(s)", materials.size());

        // Build node tree with transforms
        Matrix4f baseTransform = new Matrix4f().scale(10f);
        rootNode = buildSceneTree(scene.mRootNode(), scene, baseTransform);

        // Free the native scene
        Assimp.aiReleaseImport(scene);

        logger.info("Model '{}' loaded: {} mesh(es), {} material(s)",
                name, meshDataList.size(), materials.size());
    }

    /**
     * Recursively build a SceneNode tree from the Assimp node hierarchy,
     * accumulating transforms and converting each referenced mesh.
     */
    private SceneNode buildSceneTree(AINode node, AIScene scene, Matrix4f parentTransform) {
        // Build this node's local 4×4 transform
        org.lwjgl.assimp.AIMatrix4x4 m = node.mTransformation();
        Matrix4f localTransform = new Matrix4f(
                m.a1(), m.b1(), m.c1(), m.d1(),
                m.a2(), m.b2(), m.c2(), m.d2(),
                m.a3(), m.b3(), m.c3(), m.d3(),
                m.a4(), m.b4(), m.c4(), m.d4()
        );

        Matrix4f globalTransform = new Matrix4f(parentTransform).mul(localTransform);

        // Node name
        String nodeName = node.mName().dataString();
        if (nodeName == null || nodeName.isEmpty()) {
            nodeName = "node_" + meshDataList.size();
        }

        SceneNode sceneNode = new SceneNode(nodeName, localTransform);

        // Process each mesh referenced by this node
        int meshCount = node.mNumMeshes();
        for (int i = 0; i < meshCount; i++) {
            int meshIndex = node.mMeshes().get(i);
            AIMesh aiMesh = AIMesh.create(scene.mMeshes().get(meshIndex));
            Mesh mesh = processMesh(aiMesh);
            sceneNode.addMesh(mesh);

            // Look up the material's diffuse color for this mesh
            int matIndex = aiMesh.mMaterialIndex();
            Vector3f meshColor = (matIndex >= 0 && matIndex < materials.size())
                    ? materials.get(matIndex).getDiffuseColor()
                    : new Vector3f(0.8f, 0.8f, 0.8f);

            MeshData meshData = new MeshData(mesh, globalTransform, meshColor);
            meshDataList.add(meshData);
        }

        // Recurse into children
        int childCount = node.mNumChildren();
        for (int i = 0; i < childCount; i++) {
            AINode child = AINode.create(node.mChildren().get(i));
            SceneNode childNode = buildSceneTree(child, scene, globalTransform);
            sceneNode.addChild(childNode);
        }

        return sceneNode;
    }

    /**
     * Convert an Assimp AIMesh into our engine's Mesh (uploads to GPU).
     */
    private Mesh processMesh(AIMesh aiMesh) {
        int vertexCount = aiMesh.mNumVertices();

        // Positions
        float[] positions = new float[vertexCount * 3];
        AIVector3D.Buffer posBuffer = aiMesh.mVertices();
        for (int i = 0; i < vertexCount; i++) {
            AIVector3D v = posBuffer.get(i);
            positions[i * 3] = v.x();
            positions[i * 3 + 1] = v.y();
            positions[i * 3 + 2] = v.z();
        }

        // Normals
        float[] normals = new float[vertexCount * 3];
        AIVector3D.Buffer normBuffer = aiMesh.mNormals();
        if (normBuffer != null) {
            for (int i = 0; i < vertexCount; i++) {
                AIVector3D n = normBuffer.get(i);
                normals[i * 3] = n.x();
                normals[i * 3 + 1] = n.y();
                normals[i * 3 + 2] = n.z();
            }
        }

        // Texture coordinates
        float[] texCoords = new float[vertexCount * 2];
        if (aiMesh.mNumUVComponents(0) > 0) {
            AIVector3D.Buffer texBuffer = aiMesh.mTextureCoords(0);
            if (texBuffer != null) {
                for (int i = 0; i < vertexCount; i++) {
                    AIVector3D uv = texBuffer.get(i);
                    texCoords[i * 2] = uv.x();
                    texCoords[i * 2 + 1] = uv.y();
                }
                logger.debug("  Loaded UV coordinates for mesh with {} vertices", vertexCount);
            }
        } else {
            logger.debug("  No UV coordinates found for mesh, using default");
        }

        // Indices
        int faceCount = aiMesh.mNumFaces();
        List<Integer> indexList = new ArrayList<>();
        AIFace.Buffer faceBuffer = aiMesh.mFaces();
        for (int i = 0; i < faceCount; i++) {
            AIFace face = faceBuffer.get(i);
            IntBuffer idxBuf = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++) {
                indexList.add(idxBuf.get(j));
            }
        }
        int[] indices = indexList.stream().mapToInt(Integer::intValue).toArray();

        // Mesh name - fall back to a generated name if Assimp gives us an empty one
        String meshName = aiMesh.mName().dataString();
        System.err.println("Processing mesh: " + meshName + " with " + vertexCount + " vertices and " + indices.length + " indices");
        if (meshName == null || meshName.isEmpty()) {
            meshName = name + "_mesh_" + meshDataList.size();
        }

        logger.debug("  Mesh '{}': {} vertices, {} indices", meshName, vertexCount, indices.length);
        return new Mesh(positions, normals, texCoords, indices, meshName);
    }

    /**
     * Extract material colours from an Assimp material.
     */
    private MaterialInfo processMaterial(AIMaterial aiMaterial) {
        // Name
        AIString matName = AIString.calloc();
        Assimp.aiGetMaterialString(aiMaterial, Assimp.AI_MATKEY_NAME, 0, 0, matName);
        String materialName = matName.dataString();
        matName.free();

        // Diffuse
        AIColor4D color = AIColor4D.calloc();
        Vector3f diffuseColor = new Vector3f(0.8f, 0.8f, 0.8f);
        if (Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_DIFFUSE, 0, 0, color) == Assimp.aiReturn_SUCCESS) {
            diffuseColor.set(color.r(), color.g(), color.b());
        }

        // Specular
        Vector3f specularColor = new Vector3f(1.0f, 1.0f, 1.0f);
        if (Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_SPECULAR, 0, 0, color) == Assimp.aiReturn_SUCCESS) {
            specularColor.set(color.r(), color.g(), color.b());
        }

        // Ambient
        Vector3f ambientColor = new Vector3f(0.2f, 0.2f, 0.2f);
        if (Assimp.aiGetMaterialColor(aiMaterial, Assimp.AI_MATKEY_COLOR_AMBIENT, 0, 0, color) == Assimp.aiReturn_SUCCESS) {
            ambientColor.set(color.r(), color.g(), color.b());
        }
        color.free();

        logger.debug("  Material '{}': diffuse={}, specular={}, ambient={}",
                materialName, diffuseColor, specularColor, ambientColor);

        return new MaterialInfo(materialName, diffuseColor, specularColor, ambientColor);
    }

    public String getName() {
        return name;
    }

    /**
     * All mesh + transform pairs that make up this model.
     */
    public List<MeshData> getMeshDataList() {
        return meshDataList;
    }

    /**
     * Alias used by SimulationEngine for rendering.
     */
    public List<MeshData> getMeshesWithTransforms() {
        return meshDataList;
    }

    /**
     * The root of the scene-node hierarchy (preserves the file's node tree).
     */
    public SceneNode getRootNode() {
        return rootNode;
    }

    /**
     * Materials extracted from the model file.
     */
    public List<MaterialInfo> getMaterials() {
        return materials;
    }

    public int getMeshCount() {
        return meshDataList.size();
    }

    /**
     * Render every mesh in this model.
     */
    public void render() {
        for (MeshData md : meshDataList) {
            md.getMesh().render();
        }
    }

    /**
     * Free all GPU resources held by this model's meshes.
     */
    public void cleanup() {
        for (MeshData md : meshDataList) {
            md.getMesh().cleanup();
        }
        meshDataList.clear();
    }

    /**
     * A node in the model's scene hierarchy. Each node has a local transform,
     * zero or more meshes, and zero or more child nodes.
     */
    public static class SceneNode {

        private final String name;
        private final Matrix4f transform;
        private final List<Mesh> meshes;
        private final List<SceneNode> children;

        public SceneNode(String name, Matrix4f transform) {
            this.name = name;
            this.transform = new Matrix4f(transform);
            this.meshes = new ArrayList<>();
            this.children = new ArrayList<>();
        }

        public void addMesh(Mesh mesh) {
            meshes.add(mesh);
        }

        public void addChild(SceneNode child) {
            children.add(child);
        }

        public String getName() {
            return name;
        }

        public Matrix4f getTransform() {
            return transform;
        }

        public List<Mesh> getMeshes() {
            return meshes;
        }

        public List<SceneNode> getChildren() {
            return children;
        }

        @Override
        public String toString() {
            return "SceneNode{name='" + name + "', meshes=" + meshes.size()
                    + ", children=" + children.size() + "}";
        }
    }

    /**
     * Simple container for material colour properties extracted from the file.
     */
    public static class MaterialInfo {

        private final String name;
        private final Vector3f diffuseColor;
        private final Vector3f specularColor;
        private final Vector3f ambientColor;

        public MaterialInfo(String name, Vector3f diffuse, Vector3f specular, Vector3f ambient) {
            this.name = name;
            this.diffuseColor = diffuse;
            this.specularColor = specular;
            this.ambientColor = ambient;
        }

        public String getName() {
            return name;
        }

        public Vector3f getDiffuseColor() {
            return diffuseColor;
        }

        public Vector3f getSpecularColor() {
            return specularColor;
        }

        public Vector3f getAmbientColor() {
            return ambientColor;
        }

        @Override
        public String toString() {
            return "MaterialInfo{name='" + name
                    + "', diffuse=" + diffuseColor
                    + ", specular=" + specularColor
                    + ", ambient=" + ambientColor + "}";
        }
    }

    public void textureMesh(String textureName, String materialName, int textureId) {
        for (MeshData md : meshDataList) {
            if (md.getMesh().getMeshName().equals(materialName)) {
                md.getMesh().setTextureId(textureId);
                logger.info("Applied texture '{}' to mesh '{}'", textureName, materialName);
                return;
            }
        }
        logger.warn("No mesh found with name '{}' to apply texture '{}'", materialName, textureName);
    }

    public void changeColor(String meshName, float r, float g, float b) {
        for (MeshData md : meshDataList) {
            if (md.getMesh().getMeshName().equals(meshName)) {
                md.setColor(r, g, b);
                logger.info("Changed color of mesh '{}' to ({}, {}, {})", meshName, r, g, b);
                return;
            }
        }
        logger.warn("No mesh found with name '{}' to change color", meshName);
    }

    public int loadTexture(String texturePath) {
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        // Load image using STBImage
        ByteBuffer imageData = STBImage.stbi_load(texturePath, width, height, channels, 4);

        if (imageData == null) {
            System.err.println("Failed to load texture: " + texturePath);
            System.err.println("STB Error: " + STBImage.stbi_failure_reason());
            return -1;
        }

        // Generate texture ID
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Upload texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0),
                0, GL_RGBA, GL_UNSIGNED_BYTE, imageData);

        // Generate mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);

        // Free image data
        STBImage.stbi_image_free(imageData);

        // Unbind texture
        glBindTexture(GL_TEXTURE_2D, 0);

        System.out.println("Loaded texture: " + texturePath + " with ID: " + textureId);
        return textureId;
    }


    /**
     * Create a new mesh group with the given name.
     */
    public void createGroup(String groupName) {
        meshGroups.put(groupName, new ArrayList<>());
        logger.info("Created group '{}'", groupName);
    }

    /**
     * Add meshes to a group by their names.
     */
    public void addMeshToGroup(String[] meshNames, String groupName) {
        List<String> group = meshGroups.get(groupName);
        if (group == null) {
            logger.warn("Group '{}' does not exist", groupName);
            return;
        }
        for (String meshName : meshNames) {
            // Find the mesh and rename it with group prefix
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(meshName)) {
                    String newName = groupName + "_" + meshName;
                    md.getMesh().setDisplayName(newName);
                    group.add(newName);
                    logger.info("Added mesh '{}' to group '{}' (renamed to '{}')", meshName, groupName, newName);
                    break;
                }
            }
        }
    }

    /**
     * Copy an entire group to a new group name, creating independent copies of
     * all meshes.
     */
    public void copyGroup(String sourceGroup, String targetGroup) {
        List<String> source = meshGroups.get(sourceGroup);
        if (source == null) {
            logger.warn("Source group '{}' does not exist", sourceGroup);
            return;
        }

        List<String> target = new ArrayList<>();
        meshGroups.put(targetGroup, target);

        // For each mesh in the source group, create a full copy
        for (String sourceMeshName : source) {
            // Find the source MeshData
            MeshData sourceMeshData = null;
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(sourceMeshName)) {
                    sourceMeshData = md;
                    break;
                }
            }

            if (sourceMeshData != null) {
                // Clone the mesh with a new name
                String newMeshName = targetGroup + "_" + sourceMeshData.getMesh().getMeshName();
                Mesh clonedMesh = sourceMeshData.getMesh().clone(newMeshName);

                // Create a new MeshData with copied transform and color
                MeshData copiedMeshData = new MeshData(
                        clonedMesh,
                        new Matrix4f(sourceMeshData.getLocalTransform()),
                        new Vector3f(sourceMeshData.getColor())
                );

                // Add to meshDataList and target group
                meshDataList.add(copiedMeshData);
                target.add(newMeshName);

                logger.info("Copied mesh '{}' to group '{}' (new name: '{}')",
                        sourceMeshName, targetGroup, newMeshName);
            }
        }

        logger.info("Copied group '{}' to '{}' with full mesh copies", sourceGroup, targetGroup);
    }

    /**
     * Get the pivot point (center) of a group of meshes.
     *
     */
    public com.jme3.math.Vector3f getGroupPosition(String groupName) {
        org.joml.Vector3f pivot = getGroupPivot(groupName);
        return new com.jme3.math.Vector3f(pivot.x, pivot.y, pivot.z);
    }

    /**
     * Get the pivot point (center) of a group of meshes.
     */
    private org.joml.Vector3f getGroupPivot(String groupName) {
        List<String> group = meshGroups.get(groupName);
        if (group == null || group.isEmpty()) {
            return new org.joml.Vector3f(0, 0, 0);
        }

        org.joml.Vector3f pivot = new org.joml.Vector3f(0, 0, 0);
        int count = 0;

        for (String meshName : group) {
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(meshName)) {
                    org.joml.Vector3f pos = new org.joml.Vector3f();
                    md.getLocalTransform().getTranslation(pos);
                    pivot.add(pos);
                    count++;
                }
            }
        }

        if (count > 0) {
            pivot.div(count);
        }
        return pivot;
    }

    /**
     * Flip (mirror) a single mesh along specified axes around the model origin.
     */
    public void flip(String meshName, boolean flipX, boolean flipY, boolean flipZ) {
        for (MeshData md : meshDataList) {
            if (md.getMesh().getMeshName().equals(meshName)) {
                Matrix4f transform = md.getLocalTransform();

                // Get current translation
                Vector3f translation = new Vector3f();
                transform.getTranslation(translation);

                // Mirror the position across the specified axes (around origin)
                if (flipX) {
                    translation.x = -translation.x;
                }
                if (flipY) {
                    translation.y = -translation.y;
                }
                if (flipZ) {
                    translation.z = -translation.z;
                }

                // Update the translation in the transform matrix
                transform.setTranslation(translation);

                logger.info("Flipped mesh '{}' position to ({}, {}, {})",
                        meshName, translation.x, translation.y, translation.z);
                return;
            }
        }
        logger.warn("Mesh '{}' not found for flip operation", meshName);
    }

    /**
     * Flip (mirror) all meshes in a group along specified axes around the model
     * origin. This mirrors the position of meshes across the specified axes.
     */
    public void flipGroup(String groupName, boolean flipX, boolean flipY, boolean flipZ) {
        List<String> group = meshGroups.get(groupName);
        if (group == null) {
            logger.warn("Group '{}' does not exist", groupName);
            return;
        }

        for (String meshName : group) {
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(meshName)) {
                    Matrix4f transform = md.getLocalTransform();

                    // Get current translation
                    Vector3f translation = new Vector3f();
                    transform.getTranslation(translation);

                    // Mirror the position across the specified axes (around origin)
                    if (flipX) {
                        translation.x = -translation.x;
                    }
                    if (flipY) {
                        translation.y = -translation.y;
                    }
                    if (flipZ) {
                        translation.z = -translation.z;
                    }

                    // Update the translation in the transform matrix
                    transform.setTranslation(translation);

                    logger.info("Flipped mesh '{}' position to ({}, {}, {})",
                            meshName, translation.x, translation.y, translation.z);
                }
            }
        }
    }

    /**
     * Rotate all meshes in a group by the given angles (in degrees) around the
     * group's center.
     */
    public void rotateGroup(String groupName, float angleX, float angleY, float angleZ) {
        List<String> group = meshGroups.get(groupName);
        if (group == null) {
            logger.warn("Group '{}' does not exist", groupName);
            return;
        }

        // Calculate the center of the group
        Vector3f center = new Vector3f();
        int count = 0;
        for (String meshName : group) {
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(meshName)) {
                    Vector3f translation = new Vector3f();
                    md.getLocalTransform().getTranslation(translation);
                    center.add(translation);
                    count++;
                }
            }
        }
        if (count > 0) {
            center.div(count);
        }

        logger.info("Rotating group '{}' around center ({}, {}, {})", groupName, center.x, center.y, center.z);

        // Use the calculated center as pivot
        rotateGroupAroundPivot(groupName, angleX, angleY, angleZ, center);
    }

    /**
     * Rotate all meshes in a group around a specified pivot point.
     */
    public void rotateGroupAroundPivot(String groupName, float angleX, float angleY, float angleZ, Vector3f pivot) {
        List<String> group = meshGroups.get(groupName);
        if (group == null) {
            logger.warn("Group '{}' does not exist", groupName);
            return;
        }

        // Convert degrees to radians
        float radX = (float) Math.toRadians(angleX);
        float radY = (float) Math.toRadians(angleY);
        float radZ = (float) Math.toRadians(angleZ);

        for (String meshName : group) {
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(meshName)) {
                    Matrix4f transform = md.getLocalTransform();

                    // Get current translation
                    Vector3f translation = new Vector3f();
                    transform.getTranslation(translation);

                    // Translate to pivot origin
                    translation.sub(pivot);

                    // Apply rotation
                    Matrix4f rotation = new Matrix4f().rotateXYZ(radX, radY, radZ);
                    rotation.transformPosition(translation);

                    // Translate back from pivot
                    translation.add(pivot);

                    // Update the translation
                    transform.setTranslation(translation);

                    // Also rotate the mesh's orientation
                    transform.rotateXYZ(radX, radY, radZ);

                    logger.info("Rotated mesh '{}' around pivot ({}, {}, {})",
                            meshName, pivot.x, pivot.y, pivot.z);
                }
            }
        }
    }

    /**
     * Translate all meshes in a group by the given offset.
     */
    public void translateGroup(String groupName, float x, float y, float z) {
        List<String> group = meshGroups.get(groupName);
        if (group == null) {
            logger.warn("Group '{}' does not exist", groupName);
            return;
        }

        for (String meshName : group) {
            for (MeshData md : meshDataList) {
                if (md.getMesh().getMeshName().equals(meshName)) {
                    Matrix4f transform = md.getLocalTransform();

                    // Get current translation and add offset
                    Vector3f translation = new Vector3f();
                    transform.getTranslation(translation);
                    translation.add(x, y, z);

                    // Update the translation
                    transform.setTranslation(translation);

                    logger.info("Translated mesh '{}' to ({}, {}, {})",
                            meshName, translation.x, translation.y, translation.z);
                }
            }
        }
    }


    /**
     * Add a debug sphere marker at the specified position.
     */
    public void addDebugSphere(String name, Vector3f position, float radius, Vector3f color) {
        Mesh marker = Mesh.createSphere(radius, 12, 8);
        marker.setDisplayName("debug_" + name);

        Matrix4f transform = new Matrix4f().identity().translate(position);
        MeshData meshData = new MeshData(marker, transform, color);
        meshDataList.add(meshData);

        logger.debug("Added debug sphere '{}' at position ({}, {}, {}) with radius {}",
                name, position.x, position.y, position.z, radius);
    }

    /**
     * Add a debug marker (small cube) at the specified position.
     */
    public void addDebugMarker(String name, com.jme3.math.Vector3f position, float size, com.jme3.math.Vector3f color) {
        Mesh marker = Mesh.createCube(size);
        marker.setDisplayName("debug_" + name);

        Matrix4f transform = new Matrix4f().identity().translate(position.x, position.y, position.z);
        MeshData meshData = new MeshData(marker, transform, new org.joml.Vector3f(color.x, color.y, color.z));
        meshDataList.add(meshData);

        logger.info("Added debug marker '{}' at position ({}, {}, {})", name, position.x, position.y, position.z);
    }

    /**
     * Add a debug box at the specified center position with given half-extents.
     * Useful for visualizing axis-aligned bounding boxes (AABB).
     */
    public void addDebugBox(String name, com.jme3.math.Vector3f center, com.jme3.math.Vector3f halfExtents, com.jme3.math.Vector3f color) {
        // Create a box with dimensions based on half-extents (full dimensions = 2 * halfExtents)
        Mesh boxMesh = Mesh.createBox(halfExtents.x * 2, halfExtents.y * 2, halfExtents.z * 2);
        boxMesh.setDisplayName("debug_" + name);

        // Position at the center
        Matrix4f transform = new Matrix4f().identity().translate(center.x, center.y, center.z);
        MeshData meshData = new MeshData(boxMesh, transform, new org.joml.Vector3f(color.x, color.y, color.z));
        meshDataList.add(meshData);

        logger.debug("Added debug box '{}' at center ({}, {}, {}) with half-extents ({}, {}, {})",
                name, center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z);
    }

    /**
     * Remove all debug markers from the model.
     */
    public void clearDebugMarkers() {
        meshDataList.removeIf(md -> md.getMesh().getMeshName().startsWith("debug_"));
        logger.info("Cleared all debug markers");
    }

    public void printSceneHierarchy() {
        logger.info("=== Scene Hierarchy for Model '{}' ===", name);
        if (rootNode != null) {
            printNodeTree(rootNode, 0);
        } else {
            logger.warn("Root node is null");
        }
    }

    private void printNodeTree(SceneNode node, int depth) {
        String indent = "  ".repeat(depth);
        logger.info("{}Node: {} (meshes: {}, children: {})",
                indent, node.getName(), node.getMeshes().size(), node.getChildren().size());

        for (Mesh mesh : node.getMeshes()) {
            logger.info("{}  - Mesh: {}, at {}", indent, mesh.getMeshName(), mesh.getPosition());
        }

        for (SceneNode child : node.getChildren()) {
            printNodeTree(child, depth + 1);
        }
    }

    public void printMeshDetails() {
        logger.info("=== Mesh Details for Model '{}' ===", name);
        for (int i = 0; i < meshDataList.size(); i++) {
            MeshData md = meshDataList.get(i);
            Vector3f pos = new Vector3f();
            md.getLocalTransform().getTranslation(pos);
            logger.info("[{}] Mesh: {} | Position: ({}, {}, {}) | Color: {}",
                    i, md.getMesh().getMeshName(), pos.x, pos.y, pos.z, md.getColor());
        }
    }
}
