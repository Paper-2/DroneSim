package com.paperpiper.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glCullFace;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenGL Renderer for the drone simulator.
 */
public class Renderer {

    private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

    private ShaderProgram shaderProgram;
    private ShaderProgram skyShaderProgram;
    private ShaderProgram groundShaderProgram;
    private Matrix4f projectionMatrix;
    private Matrix4f viewMatrix;

    // Camera
    private Camera camera;

    private float fov = 70.0f; // degrees
    private float nearPlane = 0.1f; // near clipping plane distance
    private float farPlane = 1000.0f; // far clipping plane distance

    // Sky dome
    private int skyVaoId;
    private int skyVboId;
    private int skyEboId;
    private int skyIndexCount;

    // Fog settings
    private Vector3f fogColor = new Vector3f(0.7f, 0.8f, 0.9f);
    private float fogDensity = 0.0f;
    private float fogStart = 50.0f;
    private float fogEnd = 500.0f;

    // Sky/cloud settings
    private Vector3f skyColorTop = new Vector3f(0.4f, 0.6f, 0.9f);
    private Vector3f skyColorBottom = new Vector3f(0.7f, 0.85f, 1.0f);
    private Vector3f cloudColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private float cloudCoverage = 0.3f;
    private float cloudSpeed = 1.0f;

    // Time tracking for animation
    private float time = 0.0f;

    public Renderer() {
        projectionMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        // Position camera above the drone looking down (drone spawns at 0, 20, 0)
        camera = new Camera(new Vector3f(0, 4, 0), 0f, -89f);
    }

    /**
     * Initialize the renderer
     */
    public void init() {
        logger.info("Initializing renderer...");

        // Enable multisampling (antialiasing)
        glEnable(GL_MULTISAMPLE);

        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        // Enable back-face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Set clear color (sky blue - matches fog color)
        glClearColor(fogColor.x, fogColor.y, fogColor.z, 1.0f);

        // Create main shader program
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(VERTEX_SHADER);
        shaderProgram.createFragmentShader(FRAGMENT_SHADER);
        shaderProgram.link();

        // Create main shader uniforms
        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("viewMatrix");
        shaderProgram.createUniform("modelMatrix");
        shaderProgram.createUniform("objectColor");
        shaderProgram.createUniform("objectAlpha");
        shaderProgram.createUniform("cameraPos");
        shaderProgram.createUniform("fogColor");
        shaderProgram.createUniform("fogDensity");
        shaderProgram.createUniform("fogStart");
        shaderProgram.createUniform("fogEnd");

        // Create sky shader program
        skyShaderProgram = new ShaderProgram();
        skyShaderProgram.createVertexShader(SKY_VERTEX_SHADER);
        skyShaderProgram.createFragmentShader(SKY_FRAGMENT_SHADER);
        skyShaderProgram.link();

        // Create sky shader uniforms
        skyShaderProgram.createUniform("projectionMatrix");
        skyShaderProgram.createUniform("viewMatrix");
        skyShaderProgram.createUniform("time");
        skyShaderProgram.createUniform("skyColorTop");
        skyShaderProgram.createUniform("skyColorBottom");
        skyShaderProgram.createUniform("cloudColor");
        skyShaderProgram.createUniform("cloudCoverage");
        skyShaderProgram.createUniform("cloudSpeed");

        // Create sky dome mesh
        createSkyDome();

        // Create ground shader program (checker pattern)
        groundShaderProgram = new ShaderProgram();
        groundShaderProgram.createVertexShader(GROUND_VERTEX_SHADER);
        groundShaderProgram.createFragmentShader(GROUND_FRAGMENT_SHADER);
        groundShaderProgram.link();

        // Create ground shader uniforms
        groundShaderProgram.createUniform("projectionMatrix");
        groundShaderProgram.createUniform("viewMatrix");
        groundShaderProgram.createUniform("modelMatrix");
        groundShaderProgram.createUniform("cameraPos");
        groundShaderProgram.createUniform("fogColor");
        groundShaderProgram.createUniform("fogDensity");
        groundShaderProgram.createUniform("checkerColor1");
        groundShaderProgram.createUniform("checkerColor2");
        groundShaderProgram.createUniform("checkerScale");

        logger.info("Renderer initialized");
    }

    /**
     * Create a sky dome (inverted sphere) for rendering the sky
     */
    private void createSkyDome() {
        int segments = 32;
        int rings = 16;

        // Calculate vertex count: (rings + 1) * (segments + 1)
        int vertexCount = (rings + 1) * (segments + 1);
        float[] vertices = new float[vertexCount * 3];

        // Calculate index count: rings * segments * 6
        int indexCount = rings * segments * 6;
        int[] indices = new int[indexCount];

        // Generate vertices
        int vi = 0;
        for (int r = 0; r <= rings; r++) {
            float phi = (float) Math.PI * r / rings;
            float y = (float) Math.cos(phi);
            float ringRadius = (float) Math.sin(phi);

            for (int s = 0; s <= segments; s++) {
                float theta = 2.0f * (float) Math.PI * s / segments;
                float x = ringRadius * (float) Math.cos(theta);
                float z = ringRadius * (float) Math.sin(theta);

                vertices[vi++] = x;
                vertices[vi++] = y;
                vertices[vi++] = z;
            }
        }

        // Generate indices (inverted winding for inside-facing)
        int ii = 0;
        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < segments; s++) {
                int current = r * (segments + 1) + s;
                int next = current + segments + 1;

                // Inverted winding order for inside view
                indices[ii++] = current;
                indices[ii++] = current + 1;
                indices[ii++] = next;

                indices[ii++] = next;
                indices[ii++] = current + 1;
                indices[ii++] = next + 1;
            }
        }

        skyIndexCount = indexCount;

        // Create VAO
        skyVaoId = glGenVertexArrays();
        glBindVertexArray(skyVaoId);

        // Create VBO for vertices
        skyVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, skyVboId);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // Create EBO for indices
        skyEboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, skyEboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glBindVertexArray(0);
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
        viewMatrix = camera.getViewMatrix();
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render() {
        render(0.016f); // Default to ~60fps timestep
    }

    public void render(float deltaTime) {
        time += deltaTime;
        updateView();

        // Render sky first with depth write disabled
        renderSky();

        // Re-enable depth writing for scene objects
        glDepthMask(true);
        glDepthFunc(GL_LESS);

        shaderProgram.bind();
        shaderProgram.setUniform("projectionMatrix", projectionMatrix);
        shaderProgram.setUniform("viewMatrix", viewMatrix);
        shaderProgram.setUniform("cameraPos", camera.getPosition());
        shaderProgram.setUniform("fogColor", fogColor);
        shaderProgram.setUniform("fogDensity", fogDensity);
        shaderProgram.setUniform("fogStart", fogStart);
        shaderProgram.setUniform("fogEnd", fogEnd);
    }

    private void renderSky() {
        // Disable depth writing so sky is always behind everything
        glDepthMask(false);
        glDepthFunc(GL_LEQUAL);
        // Disable culling - we're inside the sky dome
        glDisable(GL_CULL_FACE);

        skyShaderProgram.bind();
        skyShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        skyShaderProgram.setUniform("viewMatrix", viewMatrix);
        skyShaderProgram.setUniform("time", time);
        skyShaderProgram.setUniform("skyColorTop", skyColorTop);
        skyShaderProgram.setUniform("skyColorBottom", skyColorBottom);
        skyShaderProgram.setUniform("cloudColor", cloudColor);
        skyShaderProgram.setUniform("cloudCoverage", cloudCoverage);
        skyShaderProgram.setUniform("cloudSpeed", cloudSpeed);

        glBindVertexArray(skyVaoId);
        glDrawElements(GL_TRIANGLES, skyIndexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        skyShaderProgram.unbind();

        // Re-enable culling for scene geometry
        glEnable(GL_CULL_FACE);
    }

    public void renderMesh(Mesh mesh, Matrix4f modelMatrix, Vector3f color) {
        renderMesh(mesh, modelMatrix, color, 1.0f);
    }

    public void renderMesh(Mesh mesh, Matrix4f modelMatrix, Vector3f color, float alpha) {
        if (alpha < 1.0f) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false); // Don't write to depth buffer for transparent objects
        }

        shaderProgram.setUniform("modelMatrix", modelMatrix);
        shaderProgram.setUniform("objectColor", color);
        shaderProgram.setUniform("objectAlpha", alpha);
        mesh.render();

        if (alpha < 1.0f) {
            glDepthMask(true);
            glDisable(GL_BLEND);
        }
    }

    /**
     * Render the ground plane with a checker pattern.
     */
    public void renderGround(Mesh groundMesh, Matrix4f modelMatrix, Vector3f color1, Vector3f color2, float checkerScale) {
        // Switch to ground shader
        shaderProgram.unbind();
        groundShaderProgram.bind();

        groundShaderProgram.setUniform("projectionMatrix", projectionMatrix);
        groundShaderProgram.setUniform("viewMatrix", viewMatrix);
        groundShaderProgram.setUniform("modelMatrix", modelMatrix);
        groundShaderProgram.setUniform("cameraPos", camera.getPosition());
        groundShaderProgram.setUniform("fogColor", fogColor);
        groundShaderProgram.setUniform("fogDensity", fogDensity);
        groundShaderProgram.setUniform("checkerColor1", color1);
        groundShaderProgram.setUniform("checkerColor2", color2);
        groundShaderProgram.setUniform("checkerScale", checkerScale);

        groundMesh.render();

        // Switch back to main shader
        groundShaderProgram.unbind();
        shaderProgram.bind();
    }

    public void endRender() {
        shaderProgram.unbind();
    }

    public Camera getCamera() {
        return camera;
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    // Fog settings getters/setters
    public void setFogColor(Vector3f color) {
        this.fogColor = color;
    }

    public void setFogDensity(float density) {
        this.fogDensity = density;
    }

    public void setFogStart(float start) {
        this.fogStart = start;
    }

    public void setFogEnd(float end) {
        this.fogEnd = end;
    }

    // Sky settings getters/setters
    public void setSkyColorTop(Vector3f color) {
        this.skyColorTop = color;
    }

    public void setSkyColorBottom(Vector3f color) {
        this.skyColorBottom = color;
    }

    public void setCloudColor(Vector3f color) {
        this.cloudColor = color;
    }

    public void setCloudCoverage(float coverage) {
        this.cloudCoverage = coverage;
    }

    public void setCloudSpeed(float speed) {
        this.cloudSpeed = speed;
    }

    public void cleanup() {
        logger.info("Cleaning up renderer...");
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
        if (skyShaderProgram != null) {
            skyShaderProgram.cleanup();
        }
        if (groundShaderProgram != null) {
            groundShaderProgram.cleanup();
        }
        // Clean up sky dome
        glDeleteVertexArrays(skyVaoId);
        glDeleteBuffers(skyVboId);
        glDeleteBuffers(skyEboId);
    }

    // vertex shader TODO: move to separate file 
    private static final String VERTEX_SHADER = """
        #version 330 core
        
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aNormal;
        
        uniform mat4 projectionMatrix;
        uniform mat4 viewMatrix;
        uniform mat4 modelMatrix;
        uniform vec3 cameraPos;
        
        out vec3 fragNormal;
        out vec3 fragPos;
        out float fogDistance;
        
        void main() {
            vec4 worldPos = modelMatrix * vec4(aPos, 1.0);
            fragPos = worldPos.xyz;
            fragNormal = mat3(transpose(inverse(modelMatrix))) * aNormal;
            fogDistance = length(cameraPos - worldPos.xyz);
            gl_Position = projectionMatrix * viewMatrix * worldPos;
        }
        """;

    //  fragment shader TODO: move to separate file
    //  fragment shader for the drones 
    private static final String FRAGMENT_SHADER = """
        #version 330 core
        
        in vec3 fragNormal;
        in vec3 fragPos;
        in float fogDistance;
        
        uniform vec3 objectColor;
        uniform float objectAlpha;
        uniform vec3 fogColor;
        uniform float fogDensity;
        uniform float fogStart;
        uniform float fogEnd;
        
        out vec4 FragColor;
        
        void main() {
            // Simple directional light
            vec3 lightDir = normalize(vec3(1.0, 1.0, 0.5));
            vec3 norm = normalize(fragNormal);
            
            // Ambient
            float ambient = 0.3;
            
            // Diffuse
            float diff = max(dot(norm, lightDir), 0.0);
            
            vec3 litColor = (ambient + diff * 0.7) * objectColor;
            
            // Exponential fog
            float fogFactor = exp(-fogDensity * fogDistance);
            fogFactor = clamp(fogFactor, 0.0, 1.0);
            
            // Linear fog fallback (for distance-based control)
            // float fogFactor = clamp((fogEnd - fogDistance) / (fogEnd - fogStart), 0.0, 1.0);
            
            vec3 result = mix(fogColor, litColor, fogFactor);
            FragColor = vec4(result, objectAlpha);
        }
        """;

    private static final String SKY_VERTEX_SHADER = """
        #version 330 core
        
        layout (location = 0) in vec3 aPos;
        
        uniform mat4 projectionMatrix;
        uniform mat4 viewMatrix;
        
        out vec3 fragPos;
        
        void main() {
            fragPos = aPos;
            // Remove translation from view matrix for skybox effect
            mat4 rotView = mat4(mat3(viewMatrix));
            vec4 pos = projectionMatrix * rotView * vec4(aPos, 1.0);
            gl_Position = pos.xyww; // Set z = w for maximum depth
        }
        """;
    // the skybox/noise clouds was taken from  my beloved shadertoys
    private static final String SKY_FRAGMENT_SHADER = """
        #version 330 core
        
        in vec3 fragPos;
        
        uniform float time;
        uniform vec3 skyColorTop;
        uniform vec3 skyColorBottom;
        uniform vec3 cloudColor;
        uniform float cloudCoverage;
        uniform float cloudSpeed;
        
        out vec4 FragColor;
        
        // Simplex noise functions for procedural clouds
        vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
        vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }
        vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }
        
        float snoise(vec3 v) {
            const vec2 C = vec2(1.0/6.0, 1.0/3.0);
            const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);
            
            vec3 i = floor(v + dot(v, C.yyy));
            vec3 x0 = v - i + dot(i, C.xxx);
            
            vec3 g = step(x0.yzx, x0.xyz);
            vec3 l = 1.0 - g;
            vec3 i1 = min(g.xyz, l.zxy);
            vec3 i2 = max(g.xyz, l.zxy);
            
            vec3 x1 = x0 - i1 + C.xxx;
            vec3 x2 = x0 - i2 + C.yyy;
            vec3 x3 = x0 - D.yyy;
            
            i = mod289(i);
            vec4 p = permute(permute(permute(
                i.z + vec4(0.0, i1.z, i2.z, 1.0))
                + i.y + vec4(0.0, i1.y, i2.y, 1.0))
                + i.x + vec4(0.0, i1.x, i2.x, 1.0));
            
            float n_ = 0.142857142857;
            vec3 ns = n_ * D.wyz - D.xzx;
            
            vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
            
            vec4 x_ = floor(j * ns.z);
            vec4 y_ = floor(j - 7.0 * x_);
            
            vec4 x = x_ * ns.x + ns.yyyy;
            vec4 y = y_ * ns.x + ns.yyyy;
            vec4 h = 1.0 - abs(x) - abs(y);
            
            vec4 b0 = vec4(x.xy, y.xy);
            vec4 b1 = vec4(x.zw, y.zw);
            
            vec4 s0 = floor(b0) * 2.0 + 1.0;
            vec4 s1 = floor(b1) * 2.0 + 1.0;
            vec4 sh = -step(h, vec4(0.0));
            
            vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
            vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;
            
            vec3 p0 = vec3(a0.xy, h.x);
            vec3 p1 = vec3(a0.zw, h.y);
            vec3 p2 = vec3(a1.xy, h.z);
            vec3 p3 = vec3(a1.zw, h.w);
            
            vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2,p2), dot(p3,p3)));
            p0 *= norm.x;
            p1 *= norm.y;
            p2 *= norm.z;
            p3 *= norm.w;
            
            vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
            m = m * m;
            return 42.0 * dot(m*m, vec4(dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));
        }
        
        float fbm(vec3 p) {
            float value = 0.0;
            float amplitude = 0.5;
            float frequency = 1.0;
            for (int i = 0; i < 5; i++) {
                value += amplitude * snoise(p * frequency);
                amplitude *= 0.5;
                frequency *= 2.0;
            }
            return value;
        }
        
        void main() {
            vec3 dir = normalize(fragPos);
            
            // Sky gradient based on vertical direction
            float t = dir.y * 0.5 + 0.5;
            vec3 skyColor = mix(skyColorBottom, skyColorTop, t);
            
            // Only render clouds above horizon
            if (dir.y > 0.0) {
                // Project onto a dome
                vec2 uv = dir.xz / (dir.y + 0.1);
                
                // Animate clouds
                vec3 cloudPos = vec3(uv * 2.0 + time * cloudSpeed * 0.02, time * cloudSpeed * 0.01);
                
                // Multi-octave noise for clouds
                float noise = fbm(cloudPos);
                
                // Apply coverage threshold
                float clouds = smoothstep(cloudCoverage, cloudCoverage + 0.3, noise * 0.5 + 0.5);
                
                // Fade clouds near horizon
                clouds *= smoothstep(0.0, 0.2, dir.y);
                
                skyColor = mix(skyColor, cloudColor, clouds * 0.8);
            }
            
            FragColor = vec4(skyColor, 1.0);
        }
        """;

    // Ground vertex shader
    private static final String GROUND_VERTEX_SHADER = """
        #version 330 core
        
        layout (location = 0) in vec3 aPos;
        layout (location = 1) in vec3 aNormal;
        
        uniform mat4 projectionMatrix;
        uniform mat4 viewMatrix;
        uniform mat4 modelMatrix;
        uniform vec3 cameraPos;
        
        out vec3 fragPos;
        out float fogDistance;
        
        void main() {
            vec4 worldPos = modelMatrix * vec4(aPos, 1.0);
            fragPos = worldPos.xyz;
            fogDistance = length(cameraPos - worldPos.xyz);
            gl_Position = projectionMatrix * viewMatrix * worldPos;
        }
        """;

    // Ground fragment shader with checker pattern
    private static final String GROUND_FRAGMENT_SHADER = """
        #version 330 core
        
        in vec3 fragPos;
        in float fogDistance;
        
        uniform vec3 checkerColor1;
        uniform vec3 checkerColor2;
        uniform float checkerScale;
        uniform vec3 fogColor;
        uniform float fogDensity;
        
        out vec4 FragColor;
        
        void main() {
            // Create checker pattern based on world XZ position
            float x = floor(fragPos.x / checkerScale);
            float z = floor(fragPos.z / checkerScale);
            float checker = mod(x + z, 2.0);
            
            // Mix between the two colors
            vec3 groundColor = mix(checkerColor1, checkerColor2, checker);
            
            // Add simple lighting (sun from above)
            vec3 lightDir = normalize(vec3(0.3, 1.0, 0.2));
            float ambient = 0.4;
            float diffuse = 0.6 * max(dot(vec3(0.0, 1.0, 0.0), lightDir), 0.0);
            groundColor *= (ambient + diffuse);
            
            // Apply fog
            float fogFactor = exp(-fogDensity * fogDistance);
            fogFactor = clamp(fogFactor, 0.0, 1.0);
            
            vec3 result = mix(fogColor, groundColor, fogFactor);
            FragColor = vec4(result, 1.0);
        }
        """;
}
