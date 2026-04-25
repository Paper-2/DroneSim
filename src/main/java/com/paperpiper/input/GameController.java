package com.paperpiper.input;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_A;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_B;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_Y;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_1;
import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadName;
import static org.lwjgl.glfw.GLFW.glfwGetGamepadState;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickAxes;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickButtons;
import static org.lwjgl.glfw.GLFW.glfwGetJoystickName;
import static org.lwjgl.glfw.GLFW.glfwJoystickIsGamepad;
import static org.lwjgl.glfw.GLFW.glfwJoystickPresent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps GLFW joystick/gamepad input for drone control.
 *
 * Default axis mapping (Xbox / Steam Deck layout): Right trigger → throttle
 * (0..1) Right stick X → yaw Right stick Y → pitch (inverted: push up = nose
 * down) Left stick X → roll
 *
 * Buttons: A (0) → arm / disarm motors (toggle) B (1) → reset drone
 *
 * Camera controls (Steam Deck D-Pad / left trigger / Y button): D-Pad
 * Left/Right → orbit camera yaw around drone D-Pad Up/Down → orbit camera pitch
 * around drone Left trigger → zoom camera in (pull = closer) Y button → reset
 * camera to default position
 */
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    // The GLFW joystick slot (0‑15)
    private int joystickId = -1;
    private boolean connected = false;
    private String controllerName = "";

    // Axis mappings (indices into the GLFW axes array)
    private int throttleAxis = 5;   // Right trigger
    private int yawAxis = 2;   // Right stick X
    private int pitchAxis = 3;   // Right stick Y
    private int rollAxis = 0;   // Left stick X

    // Whether to invert each axis (stick‑up = negative in GLFW)
    private boolean invertThrottle = true;
    private boolean invertPitch = true;
    private boolean invertYaw = false;
    private boolean invertRoll = false;

    // Dead zone to ignore stick drift
    private float deadZone = 0.15f;

    // Current processed values (–1..1, except throttle 0..1)
    private float throttle;
    private float yaw;
    private float pitch;
    private float roll;

    // Button state for edge detection (arm toggle)
    private boolean armButtonPrev = false;
    private boolean armToggled = false;

    private boolean resetButtonPrev = false;
    private boolean resetPressed = false;

    // Camera orbit state
    private static final float CAM_ORBIT_SPEED = 3.0f;  // degrees per update tick
    private static final float CAM_MIN_DISTANCE = 2.0f;
    private static final float CAM_MAX_DISTANCE = 15.0f;
    private static final float CAM_DEFAULT_YAW = 0f;
    private static final float CAM_DEFAULT_PITCH = 25f;
    private static final float CAM_DEFAULT_DISTANCE = 8f;

    private float cameraYawOffset = CAM_DEFAULT_YAW;
    private float cameraPitchOffset = CAM_DEFAULT_PITCH;
    private float cameraDistance = CAM_DEFAULT_DISTANCE;
    private boolean cameraResetPrev = false;

    public GameController() {
        scanForController();
    }

    /**
     * Look for the first connected joystick / gamepad.
     */
    public void scanForController() {
        for (int id = GLFW_JOYSTICK_1; id <= GLFW_JOYSTICK_LAST; id++) {
            if (glfwJoystickPresent(id)) {
                joystickId = id;
                connected = true;
                controllerName = glfwGetJoystickName(id);
                if (controllerName == null) {
                    controllerName = "Unknown";
                }

                // If GLFW recognises it as a gamepad, use the standard mapping
                if (glfwJoystickIsGamepad(id)) {
                    controllerName = glfwGetGamepadName(id);
                    if (controllerName == null) {
                        controllerName = "Gamepad";
                    }
                    logger.info("Gamepad detected: '{}' (slot {})", controllerName, id);
                } else {
                    logger.info("Joystick detected: '{}' (slot {})", controllerName, id);
                }
                return;
            }
        }
        connected = false;
        joystickId = -1;
    }

    /**
     * Poll the controller and update throttle / yaw / pitch / roll values. Call
     * once per frame before reading values.
     */
    public void update() {
        if (!connected) {
            scanForController();
            if (!connected) {
                return;
            }
        }

        // Check the controller is still plugged in
        if (!glfwJoystickPresent(joystickId)) {
            logger.info("Controller '{}' disconnected", controllerName);
            connected = false;
            throttle = 0;
            yaw = 0;
            pitch = 0;
            roll = 0;
            return;
        }

        // --- Axes ---
        FloatBuffer axes;
        if (glfwJoystickIsGamepad(joystickId)) {
            // Use standard gamepad state for consistent mapping
            var state = org.lwjgl.glfw.GLFWGamepadState.calloc();
            if (glfwGetGamepadState(joystickId, state)) {
                // Right trigger for throttle: GLFW reports -1 (released) to +1 (fully pressed)
                float rawTrigger = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER);
                float rawRoll = state.axes(GLFW_GAMEPAD_AXIS_LEFT_X);
                float rawPitch = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_Y);
                float rawYaw = state.axes(GLFW_GAMEPAD_AXIS_RIGHT_X);

                // Remap trigger from -1..1 to 0..1
                throttle = (rawTrigger + 1f) / 2f;
                roll = applyDeadZone(invertRoll ? -rawRoll : rawRoll);
                pitch = applyDeadZone(invertPitch ? -rawPitch : rawPitch);
                yaw = applyDeadZone(invertYaw ? -rawYaw : rawYaw);

                // --- Buttons ---
                boolean armButton = state.buttons(GLFW_GAMEPAD_BUTTON_A) == GLFW_PRESS;
                boolean resetButton = state.buttons(GLFW_GAMEPAD_BUTTON_B) == GLFW_PRESS;

                armToggled = armButton && !armButtonPrev;
                armButtonPrev = armButton;

                resetPressed = resetButton && !resetButtonPrev;
                resetButtonPrev = resetButton;

                // --- Camera orbit (D-Pad + left trigger + Y) ---
                boolean dpadLeft = state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_LEFT) == GLFW_PRESS;
                boolean dpadRight = state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) == GLFW_PRESS;
                boolean dpadUp = state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_UP) == GLFW_PRESS;
                boolean dpadDown = state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_DOWN) == GLFW_PRESS;

                if (dpadLeft) {
                    cameraYawOffset -= CAM_ORBIT_SPEED;
                }
                if (dpadRight) {
                    cameraYawOffset += CAM_ORBIT_SPEED;
                }
                if (dpadUp) {
                    cameraPitchOffset = Math.min(85f, cameraPitchOffset + CAM_ORBIT_SPEED);
                }
                if (dpadDown) {
                    cameraPitchOffset = Math.max(-10f, cameraPitchOffset - CAM_ORBIT_SPEED);
                }

                // Left trigger: zoom in (pulled = closer to drone)
                float rawLeftTrigger = state.axes(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER);
                float leftTrigger = Math.max(0f, (rawLeftTrigger + 1f) / 2f);  // remap -1..1 → 0..1
                cameraDistance = CAM_MAX_DISTANCE - leftTrigger * (CAM_MAX_DISTANCE - CAM_MIN_DISTANCE);

                // Y button: reset camera
                boolean camResetButton = state.buttons(GLFW_GAMEPAD_BUTTON_Y) == GLFW_PRESS;
                if (camResetButton && !cameraResetPrev) {
                    cameraYawOffset = CAM_DEFAULT_YAW;
                    cameraPitchOffset = CAM_DEFAULT_PITCH;
                    cameraDistance = CAM_DEFAULT_DISTANCE;
                }
                cameraResetPrev = camResetButton;
            }
            state.free();
        } else {
            // Fallback: raw joystick axes
            axes = glfwGetJoystickAxes(joystickId);
            if (axes == null) {
                return;
            }

            int axisCount = axes.remaining();
            float rawThrottle = axisCount > throttleAxis ? axes.get(throttleAxis) : 0f;
            float rawRoll = axisCount > rollAxis ? axes.get(rollAxis) : 0f;
            float rawPitch = axisCount > pitchAxis ? axes.get(pitchAxis) : 0f;
            float rawYaw = axisCount > yawAxis ? axes.get(yawAxis) : 0f;

            // Remap trigger from -1..1 to 0..1
            throttle = (rawThrottle + 1f) / 2f;
            roll = applyDeadZone(invertRoll ? -rawRoll : rawRoll);
            pitch = applyDeadZone(invertPitch ? -rawPitch : rawPitch);
            yaw = applyDeadZone(invertYaw ? -rawYaw : rawYaw);

            // Buttons via raw joystick
            ByteBuffer buttons = glfwGetJoystickButtons(joystickId);
            if (buttons != null) {
                boolean armButton = buttons.remaining() > 0 && buttons.get(0) == GLFW_PRESS;
                boolean resetButton = buttons.remaining() > 1 && buttons.get(1) == GLFW_PRESS;

                armToggled = armButton && !armButtonPrev;
                armButtonPrev = armButton;
                resetPressed = resetButton && !resetButtonPrev;
                resetButtonPrev = resetButton;
            }
        }
    }

    // --- Getters -------------------------------------------------------
    /**
     * Throttle in 0..1 range.
     */
    public float getThrottle() {
        return throttle;
    }

    /**
     * Yaw in –1..1 range.
     */
    public float getYaw() {
        return yaw;
    }

    /**
     * Pitch in –1..1 range.
     */
    public float getPitch() {
        return pitch;
    }

    /**
     * Roll in –1..1 range.
     */
    public float getRoll() {
        return roll;
    }

    /**
     * True on the single frame the arm button is pressed (edge).
     */
    public boolean isArmToggled() {
        return armToggled;
    }

    /**
     * True on the single frame the reset button is pressed (edge).
     */
    public boolean isResetPressed() {
        return resetPressed;
    }

    /**
     * Camera orbit yaw offset in degrees.
     */
    public float getCameraYawOffset() {
        return cameraYawOffset;
    }

    /**
     * Camera orbit pitch offset in degrees (0 = level, positive = above).
     */
    public float getCameraPitchOffset() {
        return cameraPitchOffset;
    }

    /**
     * Camera distance from drone in metres.
     */
    public float getCameraDistance() {
        return cameraDistance;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getName() {
        return controllerName;
    }

    // --- Configuration -------------------------------------------------
    public void setDeadZone(float dz) {
        this.deadZone = Math.max(0f, dz);
    }

    public void setInvertThrottle(boolean inv) {
        this.invertThrottle = inv;
    }

    public void setInvertPitch(boolean inv) {
        this.invertPitch = inv;
    }

    public void setInvertYaw(boolean inv) {
        this.invertYaw = inv;
    }

    public void setInvertRoll(boolean inv) {
        this.invertRoll = inv;
    }

    // --- Internal ------------------------------------------------------
    private float applyDeadZone(float value) {
        if (Math.abs(value) < deadZone) {
            return 0f;
        }
        // Rescale so the usable range is still 0..1 outside the dead zone
        float sign = Math.signum(value);
        return sign * (Math.abs(value) - deadZone) / (1f - deadZone);
    }
}
