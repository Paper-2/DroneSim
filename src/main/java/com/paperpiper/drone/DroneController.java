package com.paperpiper.drone;

import java.util.Arrays;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class DroneController implements FlightController {

    // PID channel enum for state indexing
    private enum PidChannel {
        ROLL_ANGLE, PITCH_ANGLE, YAW_ANGLE,
        ROLL_RATE, PITCH_RATE, YAW_RATE,
        POS_X, POS_Y, POS_Z
    }

    // Target position (world space)
    private Vector3f targetPosition = null;
    private boolean positionHoldEnabled = false; // : if this is ever false autopilot will be disabled. This is not intended.

    // =====================================================================
    // Position → Velocity (outer P loop)
    // =====================================================================
    private float kpPos = 1.0f;   // position error (m) → desired velocity (m/s)

    private static final float MAX_VEL = 1000000.0f;   // m/s  (3D magnitude cap) or in  15*60km/h

    // =====================================================================
    // Velocity → Desired Acceleration (PID)
    // =====================================================================
    private float kpVelXZ = 1.5f;   // velocity error → desired accel (m/s²)
    private float kiVelXZ = 0.0f;
    private float kdVelXZ = 0.05f;

    // Use the SAME gains for Y so the PID output direction matches the
    // velocity-error direction — different gains distort the 3D vector,
    // causing the flight path to curve.
    private float kpVelY = 1.5f; // mismatch causes trouble btw.
    private float kiVelY = 0.0f;
    private float kdVelY = 0.05f;

    // =====================================================================
    // Physics constants (derived from propeller specs in Drone.java)
    // =====================================================================
    private static final float GRAVITY = 9.81f;  // m/s²
    private static final float DRONE_MASS = 1.5f;   // kg
    // Total max thrust across all 4 motors, derived from propeller equation
    private static final float TOTAL_MAX_THRUST = 4f * Drone.MAX_THRUST_PER_MOTOR;
    private static final float MAX_ACCEL_HORIZ = 6.0f;  // m/s² — 3D magnitude cap for desired accel
    // Fraction of max thrust budget available after motor-mixer overhead.
    // The throttle-priority mixer scales corrections instead of shifting
    // throttle, so the commanded average thrust is largely preserved.
    // A small headroom avoids driving corrections to zero at max throttle.
    private static final float MIXER_HEADROOM = 0.85f;

    // Hover command accounts for quadratic thrust curve (T ∝ command²):
    //   mg = TOTAL_MAX_THRUST × hoverCmd²  →  hoverCmd = sqrt(mg / T_max)
    private float hoverThrottle = (float) Math.sqrt(DRONE_MASS * GRAVITY / TOTAL_MAX_THRUST);

    // PID gains
    private float kpRollAngle = 4.0f;
    private float kiRollAngle = 0.0f;   // Not using integral on angle loop by default 
    private float kdRollAngle = 0.2f;

    private float kpPitchAngle = 4.0f;
    private float kiPitchAngle = 0.0f;  // Not using integral on angle loop by default 
    private float kdPitchAngle = 0.2f;  // Not using integral on angle loop by default 

    @SuppressWarnings("unused")
    private float kpYawAngle = 2.0f;
    @SuppressWarnings("unused")
    private float kiYawAngle = 0.0f;
    @SuppressWarnings("unused")
    private float kdYawAngle = 0.0f;

    //
    private float kpRollRate = 0.7f;
    private float kiRollRate = 0.1f;
    private float kdRollRate = 0.02f;

    private float kpPitchRate = 0.7f;
    private float kiPitchRate = 0.1f;
    private float kdPitchRate = 0.02f;

    private float kpYawRate = 0.5f;
    private float kiYawRate = 0.05f;
    private float kdYawRate = 0.01f;

    // PID state indexed by PidChannel.ordinal()
    private final float[] integrals = new float[PidChannel.values().length];
    private final float[] prevErrors = new float[PidChannel.values().length];

    // Self-orient fallback
    // Small stick deadzone below which the autopilot will actively level the craft.
    private static final float STICK_DEADZONE = 0.05f;

    // Track whether self-orient mode is currently active (for future enhancements)
    private boolean selfOrientActive = false;

    // Limits
    private static final float MAX_ANGLE_COMMAND = 45f;   // max tilt angle in degrees
    private static final float MAX_RATE_COMMAND = 220f;  // max angular rate in deg/s
    private static final float INTEGRAL_LIMIT = 0.3f;  // anti-windup clamp
    // Max motor-correction span (corrMax − corrMin).  Keeps average motor ≥ hover
    // when throttle is shifted to fit [0,1].  Safe upper bound ≈ 2·(1−hoverThrottle).
    private static final float MAX_CORRECTION_SPAN = 0.5f;

    // Velocity ramp-up on target change 
    private static final float VEL_RAMP_DURATION = 1.5f; // seconds
    private float timeSinceTargetChange = VEL_RAMP_DURATION; // start fully ramped

    // --- Motor output ---
    private float motorFL, motorFR, motorRL, motorRR;

    public DroneController() {
    }

    /**
     * Run one control loop iteration.
     */
    @Override
    public void update(float throttle,
            float rollInput,
            float pitchInput,
            float yawInput,
            Quaternion orientation,
            Vector3f angularVelocity,
            Vector3f position,
            Vector3f velocity,
            Vector3f acceleration,
            float deltaTime) {

        if (deltaTime <= 0f) {
            return;
        }

        // 0) Position hold — overrides Manual input.
        //    Pipeline:  pos-error → desired-vel →
        //               vel-error → desired-accel →
        //               (physics) → 
        //               desired-attitude + desired-thrust
        if (positionHoldEnabled && targetPosition != null) {
            // Advance velocity ramp timer
            timeSinceTargetChange += deltaTime;

            float errX = targetPosition.x - position.x;
            float errY = targetPosition.y - position.y;
            float errZ = targetPosition.z - position.z;

            // Stage 1: Position P → Desired velocity  
            // calculates Position error. The squared cross product gives us a distance.
            // Distance can be used to calculate a direction vector scaled to some scalar (we can cap it to max vel).
            // so the drone flies in a straight line to the target. (if the gains are tuned well that is)
            // The velocity ramp should prevents unwanted aggressive tilt after a target change,
            // letting attitude settle before high-speed flight.
            float rampFactor = clamp(timeSinceTargetChange / VEL_RAMP_DURATION, 0f, 1f); // accel factor
            float effectiveMaxVel = MAX_VEL * rampFactor; // accel factor
            float dist = (float) Math.sqrt(errX * errX + errY * errY + errZ * errZ); // direction to target times dist.  
            float desSpeed = Math.min(kpPos * dist, effectiveMaxVel);
            float desVelX, desVelY, desVelZ;
            if (dist > 0.01f) {
                float inv = desSpeed / dist;   // unit-direction × capped speed
                desVelX = errX * inv;
                desVelY = errY * inv;
                desVelZ = errZ * inv;
            } else {
                desVelX = desVelY = desVelZ = 0f;
            }

            // Stage 2: Velocity PID → Desired acceleration (m/s²)
            // Uses measured acceleration as the derivative term (D-on-measurement)
            // instead of differentiating the velocity error, giving smoother response.
            float velErrX = desVelX - velocity.x;
            float velErrY = desVelY - velocity.y;
            float velErrZ = desVelZ - velocity.z;

            float accelX = pidStepWithAccel(velErrX, acceleration.x, kpVelXZ, kiVelXZ, kdVelXZ, deltaTime, PidChannel.POS_X);
            float accelY = pidStepWithAccel(velErrY, acceleration.y, kpVelY, kiVelY, kdVelY, deltaTime, PidChannel.POS_Y);
            float accelZ = pidStepWithAccel(velErrZ, acceleration.z, kpVelXZ, kiVelXZ, kdVelXZ, deltaTime, PidChannel.POS_Z);

            // ----- Stage 2b: Clamp acceleration magnitude (preserving direction) -----
            // Instead of clamping each axis independently (which distorts the
            // 3D direction), clamp the overall magnitude so the vector keeps
            // pointing toward the target.
            float desAccelMag = (float) Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
            if (desAccelMag > MAX_ACCEL_HORIZ) {
                float s = MAX_ACCEL_HORIZ / desAccelMag;
                accelX *= s;
                accelY *= s;
                accelZ *= s;
            }

            // ----- Stage 3: Acceleration → Attitude + Thrust (physics) -----
            // The total acceleration the drone must produce:
            //   a_total = (accelX, accelY + g, accelZ)
            // because we need to counteract gravity in the vertical component.
            float totalY = accelY + GRAVITY;   // net upward accel required

            // Required thrust magnitude: F = m * |a_total|
            // Account for ~10% mixer headroom (throttle shifting for attitude corrections).
            float maxAccel = (TOTAL_MAX_THRUST * MIXER_HEADROOM) / DRONE_MASS;
            float accelMag = (float) Math.sqrt(accelX * accelX + totalY * totalY + accelZ * accelZ);

            if (accelMag > maxAccel) {
                // Thrust-limited: scale the desired acceleration uniformly so
                // the TOTAL vector (including gravity) fits the thrust budget.
                // Solve   |( k·ax,  k·ay + g,  k·az )| = maxAccel   for k:
                //   k²(ax²+ay²+az²) + 2·k·ay·g + g² − maxAccel² = 0
                //   k = (−B + √(B² − 4AC)) / 2A   where A = ax²+ay²+az², B = 2·ay·g, C = g² − maxAccel²
                float A = accelX * accelX + accelY * accelY + accelZ * accelZ;
                float B = 2f * accelY * GRAVITY;
                float C = GRAVITY * GRAVITY - maxAccel * maxAccel;
                float disc = B * B - 4f * A * C;
                if (disc >= 0f && A > 0.001f) {
                    float k = (-B + (float) Math.sqrt(disc)) / (2f * A);
                    k = clamp(k, 0f, 1f);
                    accelX *= k;
                    accelY *= k;
                    accelZ *= k;
                }
                totalY = accelY + GRAVITY;
                // Quadratic thrust curve: cmd = sqrt(F / F_max)
                throttle = clamp((float) Math.sqrt((DRONE_MASS * maxAccel) / TOTAL_MAX_THRUST), 0f, 1f);
            } else {
                throttle = clamp((float) Math.sqrt((DRONE_MASS * accelMag) / TOTAL_MAX_THRUST), 0f, 1f);
            }

            // Required tilt angles (small-angle: tilt = atan2(a_horiz, a_vert))
            //   Pitch (nose-up → −X accel)  →  desired pitch = atan2(−accelX, totalY)
            //   Roll  (tilt-right → +Z accel) → desired roll = atan2(accelZ, totalY)
            if (totalY > 0.1f) {  // only compute tilt when producing upward thrust
                float pitchRad = (float) Math.atan2(-accelX, totalY);
                float rollRad = (float) Math.atan2(accelZ, totalY);

                // Convert to normalised stick input (−1..1 maps to ±MAX_ANGLE_COMMAND)
                float maxAngleRad = (float) Math.toRadians(MAX_ANGLE_COMMAND);
                pitchInput = clamp(pitchRad / maxAngleRad, -1f, 1f);
                rollInput = clamp(rollRad / maxAngleRad, -1f, 1f);
            } else {
                // Falling / zero thrust — level out
                pitchInput = 0f;
                rollInput = 0f;
            }
        }

        // --- Extract current Euler angles from quaternion components ---
        float qx = orientation.getX();
        float qy = orientation.getY();
        float qz = orientation.getZ();
        float qw = orientation.getW();

        // Roll  (X axis rotation)
        float sinRoll = 2f * (qw * qx + qy * qz);
        float cosRoll = 1f - 2f * (qx * qx + qy * qy);
        float currentRoll = (float) Math.toDegrees(Math.atan2(sinRoll, cosRoll));

        // Pitch (Z axis rotation)
        float sinPitch = 2f * (qw * qz + qx * qy);
        float cosPitch = 1f - 2f * (qy * qy + qz * qz);
        float currentPitch = (float) Math.toDegrees(Math.atan2(sinPitch, cosPitch));

        // Yaw   (Y axis rotation) — not used in rate-mode yaw but kept for future
        float sinYawP = 2f * (qw * qy - qz * qx);
        sinYawP = Math.max(-1f, Math.min(1f, sinYawP)); // clamp for asin safety
        @SuppressWarnings("unused")
        float currentYaw = (float) Math.toDegrees(Math.asin(sinYawP));

        // Current angular rates (convert to body-local approx)
        float gyroRoll = angularVelocity.x;
        float gyroPitch = angularVelocity.z;
        float gyroYaw = angularVelocity.y;

        // If autopilot (position hold / target) is inactive, prefer "self-orient"
        // behaviour: treat small/no stick inputs as a request to stabilise/level the
        // vehicle so the angle PID focuses on keeping the drone upright.
        boolean autopilotInactive = !positionHoldEnabled && targetPosition == null;
        if (autopilotInactive) {
            // Entering/exiting flag (kept for future use)
            if (!selfOrientActive) {
                selfOrientActive = true;
                // Clear any leftover position integrators so controller focuses on attitude
                integrals[PidChannel.POS_X.ordinal()] = 0f;
                integrals[PidChannel.POS_Y.ordinal()] = 0f;
                integrals[PidChannel.POS_Z.ordinal()] = 0f;
            }
            // Small-stick deadzone: zero tiny/manual noise so we actively level.
            if (Math.abs(rollInput) < STICK_DEADZONE) {
                rollInput = 0f;
            }
            if (Math.abs(pitchInput) < STICK_DEADZONE) {
                pitchInput = 0f;
            }
            if (Math.abs(yawInput) < STICK_DEADZONE) {
                yawInput = 0f; // rate-PID will drive gyroYaw→0 which holds heading
            }
        } else {
            selfOrientActive = false;
        }

        // Desired angles from stick input
        float desiredRoll = rollInput * MAX_ANGLE_COMMAND; // degrees
        float desiredPitch = pitchInput * MAX_ANGLE_COMMAND;
        // Yaw is rate-mode: stick directly commands yaw rate
        float desiredYawRate = yawInput * MAX_RATE_COMMAND;

        // 1) Attitude (angle) PID → outputs desired angular rates
        float errorRollAngle = desiredRoll - currentRoll;
        float errorPitchAngle = desiredPitch - currentPitch;
        @SuppressWarnings("unused")
        float errorYawAngle = 0f; // Yaw uses rate mode, no angle PID

        float rateCommandRoll = pidStep(errorRollAngle, kpRollAngle, kiRollAngle, kdRollAngle,
                deltaTime, PidChannel.ROLL_ANGLE);
        float rateCommandPitch = pidStep(errorPitchAngle, kpPitchAngle, kiPitchAngle, kdPitchAngle,
                deltaTime, PidChannel.PITCH_ANGLE);

        // Clamp rate commands
        rateCommandRoll = clamp(rateCommandRoll, -MAX_RATE_COMMAND, MAX_RATE_COMMAND);
        rateCommandPitch = clamp(rateCommandPitch, -MAX_RATE_COMMAND, MAX_RATE_COMMAND);

        // 2) Rate (gyro) PID → outputs motor correction signals
        float errorRollRate = rateCommandRoll - gyroRoll;
        float errorPitchRate = rateCommandPitch - gyroPitch;
        float errorYawRate = desiredYawRate - gyroYaw;

        // Corrections.
        float corrRoll;
        float corrPitch;
        float corrYaw;

        corrRoll = pidStep(errorRollRate, kpRollRate, kiRollRate, kdRollRate,
                deltaTime, PidChannel.ROLL_RATE);
        corrPitch = pidStep(errorPitchRate, kpPitchRate, kiPitchRate, kdPitchRate,
                deltaTime, PidChannel.PITCH_RATE);
        corrYaw = pidStep(errorYawRate, kpYawRate, kiYawRate, kdYawRate,
                deltaTime, PidChannel.YAW_RATE);

        // 3) Motor mixer (X quad layout) — throttle-priority with
        //    bounded correction span.
        //
        //    - Scale corrections so max-min ≤ MAX_CORRECTION_SPAN.
        //    - Scale corrections further if any motor would exceed [0,1],
        //       preserving the commanded throttle (average motor output)
        //       so the position controller's thrust command is honoured.
        //    - Hard-clamp (safety net, rarely needed).
        float corrFL = +corrPitch + corrRoll + corrYaw;
        float corrFR = +corrPitch - corrRoll - corrYaw;
        float corrRL = -corrPitch + corrRoll - corrYaw;
        float corrRR = -corrPitch - corrRoll + corrYaw;

        // Limit the correction span to prevent excessive attitude torques.
        float corrMax = Math.max(Math.max(corrFL, corrFR), Math.max(corrRL, corrRR));
        float corrMin = Math.min(Math.min(corrFL, corrFR), Math.min(corrRL, corrRR));
        float span = corrMax - corrMin;
        if (span > MAX_CORRECTION_SPAN) {
            float s = MAX_CORRECTION_SPAN / span;
            corrFL *= s;
            corrFR *= s;
            corrRL *= s;
            corrRR *= s;
            corrMax *= s;
            corrMin *= s;
        }

        // Scale corrections to keep motors within [0, 1].
        // This preserves the commanded average thrust for straight-line flight.
        float thr = throttle;
        float corrScale = 1.0f;
        if (corrMax > 0f && thr + corrMax > 1f) {
            corrScale = Math.min(corrScale, (1f - thr) / corrMax);
        }
        if (corrMin < 0f && thr + corrMin < 0f) {
            corrScale = Math.min(corrScale, thr / (-corrMin));
        }
        corrScale = Math.max(corrScale, 0f);

        motorFL = clamp(thr + corrFL * corrScale, 0f, 1f);
        motorFR = clamp(thr + corrFR * corrScale, 0f, 1f);
        motorRL = clamp(thr + corrRL * corrScale, 0f, 1f);
        motorRR = clamp(thr + corrRR * corrScale, 0f, 1f);
    }

    // Target position control
    @Override
    public void setTargetPosition(Vector3f target) {
        this.targetPosition = target != null ? new Vector3f(target) : null;
        this.positionHoldEnabled = (target != null);
        // Reset all PID integrators and previous errors so the
        // controller starts completely fresh for the new trajectory.
        Arrays.fill(integrals, 0f);
        Arrays.fill(prevErrors, 0f);
        // Start the velocity ramp so the drone gradually accelerates
        // toward the new target instead of tilting aggressively at once.
        timeSinceTargetChange = 0f;
    }

    @Override
    public Vector3f getTargetPosition() {
        return targetPosition != null ? new Vector3f(targetPosition) : null;
    }

    @Override
    public void setPositionHoldEnabled(boolean enabled) {
        this.positionHoldEnabled = enabled;
    }

    @Override
    public boolean isPositionHoldEnabled() {
        return positionHoldEnabled;
    }

    public void setHoverThrottle(float t) {
        this.hoverThrottle = clamp(t, 0f, 1f);
    }

    public void setPositionGains(float kp, float kpVel, float kiVel) {
        kpPos = kp;
        kpVelXZ = kpVel;
        kiVelXZ = kiVel;
    }

    public void setVelocityGainsY(float kpVel, float kiVel) {
        kpVelY = kpVel;
        kiVelY = kiVel;
    }

    @Override
    public void reset() {
        Arrays.fill(integrals, 0f);
        Arrays.fill(prevErrors, 0f);
        motorFL = motorFR = motorRL = motorRR = 0f;
        timeSinceTargetChange = VEL_RAMP_DURATION; // fully ramped
    }

    // Motor outputs
    @Override
    public float getMotorFL() {
        return motorFL;
    }

    @Override
    public float getMotorFR() {
        return motorFR;
    }

    @Override
    public float getMotorRL() {
        return motorRL;
    }

    @Override
    public float getMotorRR() {
        return motorRR;
    }

    // PID gain setters (for runtime tuning) 
    public void setAngleGains(float kp, float ki, float kd) {
        kpRollAngle = kpPitchAngle = kp;
        kiRollAngle = kiPitchAngle = ki;
        kdRollAngle = kdPitchAngle = kd;
    }

    public void setYawAngleGains(float kp, float ki, float kd) {
        kpYawAngle = kp;
        kiYawAngle = ki;
        kdYawAngle = kd;
    }

    public void setRateGains(float kp, float ki, float kd) {
        kpRollRate = kpPitchRate = kp;
        kiRollRate = kiPitchRate = ki;
        kdRollRate = kdPitchRate = kd;
    }

    public void setYawRateGains(float kp, float ki, float kd) {
        kpYawRate = kp;
        kiYawRate = ki;
        kdYawRate = kd;
    }

    // Internal helpers 
    private float pidStepWithAccel(float error, float measuredAccel,
            float kp, float ki, float kd, float dt, PidChannel channel) {
        float integral = integrals[channel.ordinal()] + error * dt;
        integral = clamp(integral, -INTEGRAL_LIMIT, INTEGRAL_LIMIT);
        integrals[channel.ordinal()] = integral;

        // Use negative measured acceleration as derivative feedback
        float derivative = -measuredAccel;

        return kp * error + ki * integral + kd * derivative;
    }

    /**
     * Computes the PID formula for the given values
     */
    private float pidStep(float error, float kp, float ki, float kd,
            float dt, PidChannel channel) {
        int idx = channel.ordinal();
        float integral = integrals[idx] + error * dt;
        integral = clamp(integral, -INTEGRAL_LIMIT, INTEGRAL_LIMIT);
        integrals[idx] = integral;

        float derivative = (error - prevErrors[idx]) / dt;
        prevErrors[idx] = error;

        return kp * error + ki * integral + kd * derivative;
    }

    // Util. TODO: move to math package
    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
