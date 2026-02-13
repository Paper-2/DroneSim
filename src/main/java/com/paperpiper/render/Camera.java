package com.paperpiper.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * FPS-style camera with WASD movement and mouse look.
 */
public class Camera {

    // Camera position and orientation
    private final Vector3f position;
    private float yaw;   // Rotation around Y axis (left/right)
    private float pitch; // Rotation around X axis (up/down)

    // Camera vectors
    private final Vector3f front;
    private final Vector3f right;
    private final Vector3f up;
    private final Vector3f worldUp;

    // Movement settings
    private float moveSpeed = 10.0f;
    private float mouseSensitivity = 0.1f;
    private float sprintMultiplier = 1.25f;

    // Mouse state
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;

    // View matrix
    private final Matrix4f viewMatrix;

    public Camera() {
        this(new Vector3f(0, 5, 15), 90.0f, 15.0f);
    }

    public Camera(Vector3f position, float yaw, float pitch) {
        this.position = new Vector3f(position);
        this.yaw = yaw;
        this.pitch = pitch;

        this.front = new Vector3f(0, 0, -1);
        this.right = new Vector3f(1, 0, 0);
        this.up = new Vector3f(0, 1, 0);
        this.worldUp = new Vector3f(0, 1, 0);

        this.viewMatrix = new Matrix4f();

        updateCameraVectors();
    }

    /**
     * Process keyboard input for movement.
     */
    public void processKeyboard(boolean forward, boolean backward, boolean left, boolean right,
                                 boolean up, boolean down, boolean sprint, float deltaTime) {
        float velocity = moveSpeed * deltaTime;
        if (sprint) {
            velocity *= sprintMultiplier;
        }

        // Calculate horizontal front vector (ignore Y component for ground movement)
        Vector3f horizontalFront = new Vector3f(front.x, 0, front.z).normalize();

        if (forward) {
            position.add(new Vector3f(horizontalFront).mul(velocity));
        }
        if (backward) {
            position.sub(new Vector3f(horizontalFront).mul(velocity));
        }
        if (left) {
            position.sub(new Vector3f(this.right).mul(velocity));
        }
        if (right) {
            position.add(new Vector3f(this.right).mul(velocity));
        }
        if (up) {
            position.add(new Vector3f(worldUp).mul(velocity));
        }
        if (down) {
            position.sub(new Vector3f(worldUp).mul(velocity));
        }

        if (position.y < 0.1f) {
            position.y = 0.15f; // Prevent going below ground
        }
        if (position.y > 1000f) {
            position.y = 1000f; // Prevent going too high
        }
        

    }

    /**
     * Process mouse movement for looking around.
     */
    public void processMouseMovement(double mouseX, double mouseY) {
        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
            return;
        }

        double xOffset = mouseX - lastMouseX;
        double yOffset = lastMouseY - mouseY; // Reversed: y ranges bottom to top

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        xOffset *= mouseSensitivity;
        yOffset *= mouseSensitivity;

        yaw += xOffset;
        pitch += yOffset;

        // Constrain pitch to avoid flipping
        if (pitch > 89.0f) {
            pitch = 89.0f;
        }
        if (pitch < -89.0f) {
            pitch = -89.0f;
        }

        updateCameraVectors();
    }

    /**
     * Reset the first mouse flag (call when mouse is re-captured).
     */
    public void resetMouseState() {
        firstMouse = true;
    }

    /**
     * Update camera vectors based on yaw and pitch.
     */
    private void updateCameraVectors() {
        // Calculate new front vector
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        front.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
        front.y = (float) Math.sin(pitchRad);
        front.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
        front.normalize();

        // Recalculate right and up vectors
        front.cross(worldUp, right);
        right.normalize();

        right.cross(front, up);
        up.normalize();
    }

    /**
     * Get the view matrix for rendering.
     */
    public Matrix4f getViewMatrix() {
        Vector3f target = new Vector3f(position).add(front);
        viewMatrix.identity().lookAt(position, target, up);
        return viewMatrix;
    }

    // Getters and setters
    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position.set(position);
    }

    public Vector3f getFront() {
        return front;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        updateCameraVectors();
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
        updateCameraVectors();
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = mouseSensitivity;
    }
}
