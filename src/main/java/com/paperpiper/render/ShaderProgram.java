package com.paperpiper.render;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VALIDATE_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glValidateProgram;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenGL Shader Program wrapper.
 */
public class ShaderProgram {
    
    private static final Logger logger = LoggerFactory.getLogger(ShaderProgram.class);
    
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;
    
    private final Map<String, Integer> uniforms;
    
    public ShaderProgram() {
        uniforms = new HashMap<>();
    }
    
    /**
     * Create vertex shader from source
     */
    public void createVertexShader(String shaderCode) {
        vertexShaderId = createShader(shaderCode, GL_VERTEX_SHADER);
    }
    
    /**
     * Create fragment shader from source
     */
    public void createFragmentShader(String shaderCode) {
        fragmentShaderId = createShader(shaderCode, GL_FRAGMENT_SHADER);
    }
    
    /**
     * Create shader of given type
     */
    private int createShader(String shaderCode, int shaderType) {
        int shaderId = glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new RuntimeException("Error creating shader. Type: " + shaderType);
        }
        
        glShaderSource(shaderId, shaderCode);
        glCompileShader(shaderId);
        
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shaderId, 1024);
            throw new RuntimeException("Error compiling shader: " + log);
        }
        
        if (programId == 0) {
            programId = glCreateProgram();
            if (programId == 0) {
                throw new RuntimeException("Could not create shader program");
            }
        }
        
        glAttachShader(programId, shaderId);
        
        return shaderId;
    }
    
    /**
     * Link the shader program
     */
    public void link() {
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(programId, 1024);
            throw new RuntimeException("Error linking shader program: " + log);
        }
        

        if (vertexShaderId != 0) {
            glDetachShader(programId, vertexShaderId);
        }
        if (fragmentShaderId != 0) {
            glDetachShader(programId, fragmentShaderId);
        }
        
        // Validate program
        glValidateProgram(programId);
        if (glGetProgrami(programId, GL_VALIDATE_STATUS) == 0) {
            logger.warn("Warning validating shader: {}", glGetProgramInfoLog(programId, 1024));
        }
    }
    
    /**
     * Create uniform location
     */
    public void createUniform(String uniformName) {
        int uniformLocation = glGetUniformLocation(programId, uniformName);
        if (uniformLocation < 0) {
            logger.warn("Could not find uniform: {}", uniformName);
        }
        uniforms.put(uniformName, uniformLocation);
    }
    
5
    public void setUniform(String uniformName, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            value.get(fb);
            glUniformMatrix4fv(uniforms.get(uniformName), false, fb);
        }
    }
    

    public void setUniform(String uniformName, Vector3f value) {
        glUniform3f(uniforms.get(uniformName), value.x, value.y, value.z);
    }


    public void setUniform(String uniformName, int value) {
        glUniform1i(uniforms.get(uniformName), value);
    }
    

    public void setUniform(String uniformName, float value) {
        glUniform1f(uniforms.get(uniformName), value);
    }
    

    public void bind() {
        glUseProgram(programId);
    }
    

    public void unbind() {
        glUseProgram(0);
    }
    

    public void cleanup() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }
}
