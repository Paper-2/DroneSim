package com.paperpiper.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenGL Renderer for the drone simulator.
 */
public class Renderer {

    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

    private ShaderProgram shaderProgram;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    // Camera properties
    final private Vector3f cameraPosition;
    final private Vector3f cameraTarget;
    private Vector3f cameraUp;

    private float fov = 70.0f; // degrees
    private float nearPlane = 0.1f; // near clipping plane distance
    private float farPlane = 1000.0f; // far clipping plane distance

    public Renderer() {
        projectionMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        cameraPosition = new Vector3f(0, 10, 20);
        cameraTarget = new Vector3f(0, 0, 0);
        cameraUp = new Vector3f(0, 1, 0);
    }

    /**
     * Initialize the renderer
     */
    public void init() {
        logger.info("Initializing renderer...");

        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // Enable back-face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Set clear color (sky blue)
        glClearColor(0.529f, 0.808f, 0.922f, 1.0f);

        // Create shader program
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(VERTEX_SHADER);
        shaderProgram.createFragmentShader(FRAGMENT_SHADER);
        shaderProgram.link();

        // Create uniforms
        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("viewMatrix");
        shaderProgram.createUniform("modelMatrix");
        shaderProgram.createUniform("objectColor");

        logger.info("Renderer initialized");
    }

    public void updateProjection(int width, int height) {
        float aspectRatio = (float) width / height;
        projectionMatrix.identity().perspective(
                (float) Math.toRadians(fov),
                aspectRatio,
                nearPlane,
                farPlane
        );
    }

    public void updateView() {
        viewMatrix.identity().lookAt(cameraPosition, cameraTarget, cameraUp);
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render() {
        updateView();

        shaderProgram.bind();
        shaderProgram.setUniform("projectionMatrix", projectionMatrix);
        shaderProgram.setUniform("viewMatrix", viewMatrix);
    }

    public void renderMesh(Mesh mesh, Matrix4f modelMatrix, Vector3f color) {
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        shaderProgram.setUniform("objectColor", color);
        mesh.render();
    }

    public void endRender() {
        shaderProgram.unbind();
    }

    public void setCameraPosition(Vector3f position) {
        this.cameraPosition.set(position);
    }

    public void setCameraTarget(Vector3f target) {
        this.cameraTarget.set(target);
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public void cleanup() {
        logger.info("Cleaning up renderer...");
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
    }

    // vertex shader TODO: move to separate file 
    private static final String VERTEX_SHADER = """
        #version 330 core
        
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aNormal;
        
        uniform mat4 projectionMatrix;
        uniform mat4 viewMatrix;
        uniform mat4 modelMatrix;
        
        out vec3 fragNormal;
        out vec3 fragPos;
        
        void main() {
            vec4 worldPos = modelMatrix * vec4(aPos, 1.0);
            fragPos = worldPos.xyz;
            fragNormal = mat3(transpose(inverse(modelMatrix))) * aNormal;
            gl_Position = projectionMatrix * viewMatrix * worldPos;
        }
        """;

    //  fragment shader TODO: move to separate file 
    private static final String FRAGMENT_SHADER = """
        #version 330 core
        
        in vec3 fragNormal;
        in vec3 fragPos;
        
        uniform vec3 objectColor;
        
        out vec4 FragColor;
        
        void main() {
            // Simple directional light
            vec3 lightDir = normalize(vec3(1.0, 1.0, 0.5));
            vec3 norm = normalize(fragNormal);
            
            // Ambient
            float ambient = 0.3;
            
            // Diffuse
            float diff = max(dot(norm, lightDir), 0.0);
            
            vec3 result = (ambient + diff * 0.7) * objectColor;
            FragColor = vec4(result, 1.0);
        }
        """;

    public Vector3f getCameraPosition() {
        return cameraPosition;
    }
}
