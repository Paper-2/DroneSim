package com.paperpiper.drone;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape; // ▰▱▰▱
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import org.joml.Quaternionf;
import com.jme3.math.Vector3f;
import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Mesh;
import com.paperpiper.render.MeshData;
import com.paperpiper.render.Model;

// Represents a drone in the simulation
public class Drone {

    private static final Logger logger = LoggerFactory.getLogger(Drone.class);

    // debug mode - set via system property or method
    // TODO: instead of pressing F3 to toggle seeing collision shapes, just have
    // a debug mode that shows them by default. This will be easier to use and less error-prone than trying to toggle them on/off at runtime.
    private static final boolean DEBUG_MODE = Boolean.getBoolean("drone.debug");

    // physical properties
    private static final float DRONE_MASS = 1.5f; // kg
    /*
    No longer needed since I'm using an actual model. I'll delete this later.
    private static final float DRONE_WIDTH = 0.5f; // meters
    private static final float DRONE_HEIGHT = 0.15f;
    private static final float DRONE_DEPTH = 0.5f;
    */

    // motor
    private static final float MAX_THRUST = 25.0f; // N (must overcome gravity + margin)
    private static final float MAX_TORQUE = 5.0f;  // N⋅m

    // physics body
    private PhysicsRigidBody rigidBody;

    private Vector3f collisionCenter;
    private Vector3f collisionHalfExtents;

    private List<MeshCollisionBox> meshCollisionBoxes = new ArrayList<>();
    private boolean collisionBoxesVisible = false;

    private Vector3f front_left_propeller;
    private Vector3f front_right_propeller;
    private Vector3f rear_left_propeller;
    private Vector3f rear_right_propeller;

    // visual representation
    private DroneBody droneBody;
    private Model model;

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

        // Initialize visual representation
        droneBody = new DroneBody();
        model = droneBody.getModel();

        // Get propeller positions from the model's rotor groups
        // TODO: fix the names of the meshes to just search for the cylender mesh instead
        // of relying on the group names. works fine at the moment
        front_left_propeller = model.getGroupPosition("rotors_front_left");
        front_right_propeller = model.getGroupPosition("rotors_front_right");
        rear_left_propeller = model.getGroupPosition("rotors_rear_left");
        rear_right_propeller = model.getGroupPosition("rotors_rear_right");

        logger.info("Propeller positions - FL: {}, FR: {}, RL: {}, RR: {}",
                front_left_propeller, front_right_propeller, rear_left_propeller, rear_right_propeller);

        // Build compound collision shape from all meshes before adding debug markers 
        // Since those shouldn't have collisions, just a visual aid for thrust direction.
        CompoundCollisionShape compoundShape = buildCompoundCollisionShape();

        // Create rigid body with compound shape
        rigidBody = new PhysicsRigidBody(compoundShape, DRONE_MASS);
        rigidBody.setPhysicsLocation(startPosition);

        // TODO: Use realistic friction/restitution values based on drone materials.
        // Drone model is supposedly made out of PBC.
        rigidBody.setFriction(0.3f);
        rigidBody.setRestitution(0.1f);
        rigidBody.setAngularDamping(0.5f);
        rigidBody.setLinearDamping(0.1f);

        // Add to physics world
        physicsWorld.addRigidBody(rigidBody);

        // logger.info("Created compound collision shape with {} mesh boxes", meshCollisionBoxes.size());

        // Add debug markers AFTER physics setup - these are visual only, no collisions
        if (DEBUG_MODE) {
            addPropellerDebugMarkers();
        }
    }


    /**
     * Build a compound collision shape from all meshes in the model. Each mesh
     * gets its own box collision shape based on its AABB.
     */
    private CompoundCollisionShape buildCompoundCollisionShape() {
        CompoundCollisionShape compound = new CompoundCollisionShape();
        meshCollisionBoxes.clear();

        // Process each mesh in the model create a collision box for it.
        // TODO: Skip small and redundant meshes (like what were doing for debug meshes)
        for (MeshData meshData : model.getMeshDataList()) {
            Mesh mesh = meshData.getMesh();
            String meshName = mesh.getMeshName();

            // Skip debug meshes
            if (meshName.startsWith("debug_")) {
                continue;
            }

            // Calculate AABB for this mesh
            float[] positions = mesh.getPositions();
            if (positions == null || positions.length < 3) {
                continue;
            }

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            // Get the mesh's world transform 
            org.joml.Matrix4f transform = meshData.getLocalTransform();

            for (int i = 0; i < positions.length; i += 3) {
                org.joml.Vector3f v = new org.joml.Vector3f(positions[i], positions[i + 1], positions[i + 2]);
                transform.transformPosition(v); // uses joml for matrix math

                minX = Math.min(minX, v.x());
                minY = Math.min(minY, v.y());
                minZ = Math.min(minZ, v.z());
                maxX = Math.max(maxX, v.x());
                maxY = Math.max(maxY, v.y());
                maxZ = Math.max(maxZ, v.z());
            }

            // Calculate center and half-extents
            float centerX = (minX + maxX) / 2f;
            float centerY = (minY + maxY) / 2f;
            float centerZ = (minZ + maxZ) / 2f;
            float halfX = (maxX - minX) / 2f;
            float halfY = (maxY - minY) / 2f;
            float halfZ = (maxZ - minZ) / 2f;

            // Skip very small boxes (degenerate meshes)
            if (halfX < 0.001f || halfY < 0.001f || halfZ < 0.001f) {
                continue;
            }

            Vector3f center = new Vector3f(centerX, centerY, centerZ);
            Vector3f halfExtents = new Vector3f(halfX, halfY, halfZ);

            // Create box collision shape
            BoxCollisionShape boxShape = new BoxCollisionShape(halfExtents);
            compound.addChildShape(boxShape, center);

            // Store for visualization
            meshCollisionBoxes.add(new MeshCollisionBox(meshName, center, halfExtents));

            logger.debug("Added collision box for mesh '{}': center={}, halfExtents={}",
                    meshName, center, halfExtents);
        }

        // Calculate overall AABB for legacy support
        if (!meshCollisionBoxes.isEmpty()) {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

            for (MeshCollisionBox box : meshCollisionBoxes) {
                minX = Math.min(minX, box.center.x - box.halfExtents.x);
                minY = Math.min(minY, box.center.y - box.halfExtents.y);
                minZ = Math.min(minZ, box.center.z - box.halfExtents.z);
                maxX = Math.max(maxX, box.center.x + box.halfExtents.x);
                maxY = Math.max(maxY, box.center.y + box.halfExtents.y);
                maxZ = Math.max(maxZ, box.center.z + box.halfExtents.z);
            }

            collisionCenter = new Vector3f((minX + maxX) / 2f, (minY + maxY) / 2f, (minZ + maxZ) / 2f);
            collisionHalfExtents = new Vector3f((maxX - minX) / 2f, (maxY - minY) / 2f, (maxZ - minZ) / 2f);
        }

        return compound;
    }

    /**
     * Stores collision box data for a single mesh (for visualization).
     */
    public static class MeshCollisionBox {

        public final String meshName;
        public final Vector3f center;
        public final Vector3f halfExtents;

        public MeshCollisionBox(String meshName, Vector3f center, Vector3f halfExtents) {
            this.meshName = meshName;
            this.center = new Vector3f(center);
            this.halfExtents = new Vector3f(halfExtents);
        }
    }


    /**
     * Show or hide collision shape visualization. Must be called after init()
     * to visualize existing collision shapes.
     *
     */
    public void setCollisionShapesVisible(boolean visible) {
        if (model == null) {
            return;
        }

        this.collisionBoxesVisible = visible;

        // Remove existing collision debug markers
        model.getMeshDataList().removeIf(md
                -> md.getMesh().getMeshName().startsWith("debug_collision_"));

        if (visible && !meshCollisionBoxes.isEmpty()) {
            // Visualize each mesh's collision box with different colors
            int colorIndex = 0;

            // There's the opportunity to use graph theory to properly color this stuff.
            Vector3f[] colors = {
                new Vector3f(1.0f, 0.3f, 0.3f), // Red
                new Vector3f(0.3f, 1.0f, 0.3f), // Green
                new Vector3f(0.3f, 0.3f, 1.0f), // Blue
                new Vector3f(1.0f, 1.0f, 0.3f), // Yellow
                new Vector3f(1.0f, 0.3f, 1.0f), // Magenta
                new Vector3f(0.3f, 1.0f, 1.0f), // Cyan
                new Vector3f(1.0f, 0.6f, 0.3f), // Orange
                new Vector3f(0.6f, 0.3f, 1.0f) // Purple
            };

            for (MeshCollisionBox box : meshCollisionBoxes) {
                model.addDebugBox("collision_" + box.meshName, box.center, box.halfExtents, colors[colorIndex % colors.length]);
                colorIndex++;
            }

            logger.info("Collision shape visualization enabled ({} mesh boxes)", meshCollisionBoxes.size());
        } else if (visible && collisionHalfExtents != null) {
            // Fallback: Visualize the overall AABB
            model.addDebugBox("collision_aabb", collisionCenter, collisionHalfExtents, new Vector3f(0.0f, 0.5f, 1.0f));
            logger.info("Collision shape visualization enabled (AABB fallback)");
        } else {
            logger.info("Collision shape visualization disabled");
        }
    }

    /**
     * Check if collision shapes are currently visible.
     */
    public boolean isCollisionShapesVisible() {
        return collisionBoxesVisible;
    }

    /**
     * Get the list of mesh collision boxes (for external visualization or
     * debugging).
     */
    public List<MeshCollisionBox> getMeshCollisionBoxes() {
        return new ArrayList<>(meshCollisionBoxes);
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
                .rotate(new Quaternionf(rot.getX(), rot.getY(), rot.getZ(), rot.getW()));

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

    public Model getModel() {
        return model;
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

    /**
     * Add debug marker meshes at propeller positions. Uses different colors for
     * front/rear identification.
     */
    private void addPropellerDebugMarkers() {
        float markerSize = 0.05f;

        // Front propellers - green tint
        model.addDebugMarker("prop_front_left", front_left_propeller,
                markerSize, new Vector3f(0.2f, 1.0f, 0.2f));
        model.addDebugMarker("prop_front_right", front_right_propeller,
                markerSize, new Vector3f(0.2f, 0.8f, 0.2f));

        // Rear propellers - red tint
        model.addDebugMarker("prop_rear_left", rear_left_propeller,
                markerSize, new Vector3f(1.0f, 0.2f, 0.2f));
        model.addDebugMarker("prop_rear_right", rear_right_propeller,
                markerSize, new Vector3f(0.8f, 0.2f, 0.2f));

        logger.info("Debug markers added for propeller positions");
    }

    /**
     * Enable or disable debug visualization at runtime.
     */
    public void setDebugVisualization(boolean enabled) {
        if (model != null) {
            model.clearDebugMarkers();
            if (enabled) {
                addPropellerDebugMarkers();
            }
        }
    }
}
