package com.paperpiper.render;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GLFW Window wrapper for rendering context.
 */
public class Window {
    
    private static final Logger logger = LoggerFactory.getLogger(Window.class);
    
    private long handle;
    private String title;
    private int width;
    private int height;
    private boolean resized;
    
    public Window(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.resized = false;
    }
    
    /**
     * Initialize GLFW and create window
     */
    public void init() {
        logger.info("Initializing GLFW window: {}x{}", width, height);
        
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // boiler plate settings
        // TODO: make configurable later + add resizable/fullscreen options
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        // Creates window
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // Setup resize callback
        glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            this.resized = true;
        });
        
        // Setup key callback
        glfwSetKeyCallback(handle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        
        // Center window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(handle, pWidth, pHeight);
            
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            if (vidmode != null) {
                glfwSetWindowPos(
                    handle,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
                );
            }
        }
        
        // Make OpenGL context current
        glfwMakeContextCurrent(handle);
        
        // v-sync
        glfwSwapInterval(1);
        
        // Make window visible
        glfwShowWindow(handle);
        
        // Initialize OpenGL
        GL.createCapabilities();
        
        logger.info("Window created successfully");
    }
    

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }
    
    public void pollEvents() {
        glfwPollEvents();
    }
    
    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }
    
    public boolean isKeyPressed(int keyCode) {
        return glfwGetKey(handle, keyCode) == GLFW_PRESS;
    }

    public boolean isMouseButtonPressed(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }
    
    public long getHandle() {
        return handle;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean isResized() {
        return resized;
    }
    
    public void setResized(boolean resized) {
        this.resized = resized;
    }
    
    public void cleanup() {
        logger.info("Cleaning up window...");
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
