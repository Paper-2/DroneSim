package com.paperpiper.drone;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.paperpiper.physics.PhysicsWorld;

// Represents a drone in the simulation
public class Drone {

    private static final Logger logger = LoggerFactory.getLogger(Drone.class);

    // physical properties
    private static final float DRONE_MASS = 1.5f; // kg
    private static final float DRONE_WIDTH = 0.5f; // meters
    private static final float DRONE_HEIGHT = 0.15f;
    private static final float DRONE_DEPTH = 0.5f;

    // motor
    private static final float MAX_THRUST = 25.0f; // N (must overcome gravity + margin)
    private static final float MAX_TORQUE = 5.0f;  // N⋅m

    // physics body
    private PhysicsRigidBody rigidBody;

    // control inputs (0.0 to 1.0)
    private float throttle = 0.0f;
    private float pitch = 0.0f;    // Forward/backward tilt
    private float roll = 0.0f;     // Left/right tilt
    private float yaw = 0.0f;      // Rotation around vertical axis

    // motors state
    private boolean motorsArmed = false;

    // transform matrix for rendering
    private final Matrix4f modelMatrix;

    public Drone() {
        modelMatrix = new Matrix4f();
    }

    // Initialize drone in physics world
    public void init(PhysicsWorld physicsWorld, Vector3f startPosition) {
        logger.info("Initializing drone at position: {}", startPosition);

        // Create collision shape
        Vector3f halfExtents = new Vector3f(DRONE_WIDTH / 2, DRONE_HEIGHT / 2, DRONE_DEPTH / 2);
        BoxCollisionShape droneShape = new BoxCollisionShape(halfExtents);

        // Create rigid body
        rigidBody = new PhysicsRigidBody(droneShape, DRONE_MASS);
        rigidBody.setPhysicsLocation(startPosition);
        rigidBody.setFriction(0.3f);
        rigidBody.setRestitution(0.1f);


        rigidBody.setAngularDamping(0.5f);
        rigidBody.setLinearDamping(0.1f);

        // Add to physics world
        physicsWorld.addRigidBody(rigidBody);
    }

    // Update drone physics based on control inputs
    public void update(float deltaTime) {
        if (!motorsArmed || rigidBody == null) {
            return;
        }

        // Get current orientation as rotation matrix
        Quaternion rotation = rigidBody.getPhysicsRotation(null);
        Matrix3f rotMatrix = rotation.toRotationMatrix();

        Vector3f localUp = new Vector3f(0, 1, 0);
        Vector3f worldUp = new Vector3f();
        rotMatrix.mult(localUp, worldUp);

        float thrustMagnitude = throttle * MAX_THRUST;
        Vector3f thrustForce = worldUp.mult(thrustMagnitude);
        rigidBody.applyCentralForce(thrustForce);

        Vector3f torque = new Vector3f();

        torque.x = pitch * MAX_TORQUE;

        torque.z = -roll * MAX_TORQUE;

        torque.y = yaw * MAX_TORQUE;

        Vector3f worldTorque = new Vector3f();
        rotMatrix.mult(torque, worldTorque);
        rigidBody.applyTorque(worldTorque);
    }

    public Matrix4f getModelMatrix() {
        if (rigidBody == null) {
            return modelMatrix.identity();
        }

        Vector3f pos = rigidBody.getPhysicsLocation(null);

        Quaternion rot = rigidBody.getPhysicsRotation(null);

        modelMatrix.identity()
                .translate(pos.x, pos.y, pos.z)
                .rotate(new org.joml.Quaternionf(rot.getX(), rot.getY(), rot.getZ(), rot.getW()))
                .scale(DRONE_WIDTH, DRONE_HEIGHT, DRONE_DEPTH);

        return modelMatrix;
    }

    public Vector3f getPosition() {
        if (rigidBody == null) {
            return new Vector3f();
        }
        return rigidBody.getPhysicsLocation(null);
    }

    public Vector3f getVelocity() {
        if (rigidBody == null) {
            return new Vector3f();
        }
        return rigidBody.getLinearVelocity(null);
    }

    public void setThrottle(float throttle) {
        this.throttle = Math.max(0, Math.min(1, throttle));
    }

    public void setPitch(float pitch) {
        this.pitch = Math.max(-1, Math.min(1, pitch));
    }

    // Set roll (-1.0 to 1.0)
    public void setRoll(float roll) {
        this.roll = Math.max(-1, Math.min(1, roll));
    }

    // Set yaw (-1.0 to 1.0)
    public void setYaw(float yaw) {
        this.yaw = Math.max(-1, Math.min(1, yaw));
    }

    // Arm/disarm motors
    public void setMotorsArmed(boolean armed) {
        this.motorsArmed = armed;
        if (armed) {
            logger.info("Motors ARMED");
            rigidBody.activate();
        } else {
            logger.info("Motors DISARMED");
            throttle = 0;
        }
    }

    public boolean isMotorsArmed() {
        return motorsArmed;
    }

    public PhysicsRigidBody getRigidBody() {
        return rigidBody;
    }

    public void reset(Vector3f position) {
        if (rigidBody != null) {
            rigidBody.setPhysicsLocation(position);
            rigidBody.setLinearVelocity(new Vector3f(0, 0, 0));
            rigidBody.setAngularVelocity(new Vector3f(0, 0, 0));
            rigidBody.setPhysicsRotation(new Quaternion());
            motorsArmed = false;
            throttle = 0;
            pitch = 0;
            roll = 0;
            yaw = 0;
        }
    }

    public void cleanup(PhysicsWorld physicsWorld) {
        if (rigidBody != null) {
            physicsWorld.removeRigidBody(rigidBody);
            rigidBody = null;
        }
    }
}
