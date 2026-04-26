package com.paperpiper.drone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.bullet.collision.shapes.BoxCollisionShape; // ▰▱▰▱
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.paperpiper.drone.behavior.DroneBehavior;
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

    // Propeller / motor specs — thrust is computed from the standard propeller
    // equation: T = Ct × ρ × n² × D⁴  where n = RPM / 60 (revs per second).
    static final float PROP_DIAMETER = 0.254f;        // m  (10-inch propeller)
    static final float THRUST_COEFFICIENT = 0.072f;   // Ct (dimensionless, blade geometry)
    static final float AIR_DENSITY = 1.225f;           // kg/m³ (sea-level ISA)
    static final float MAX_MOTOR_RPM = 7200f;          // peak RPM at full command

    // Pre-computed constant: Ct × ρ × D⁴  (multiply by n² to get thrust)
    static final float THRUST_FACTOR = THRUST_COEFFICIENT * AIR_DENSITY
            * (float) Math.pow(PROP_DIAMETER, 4);

    // Derived max thrust per motor at full RPM (for reference / telemetry)
    static final float MAX_THRUST_PER_MOTOR = THRUST_FACTOR
            * (float) Math.pow(MAX_MOTOR_RPM / 60.0, 2);

    private static final float MAX_TORQUE = 5.0f;  // N⋅m

    // Per-motor RPM (derived each frame from motor command)
    private float rpmFL, rpmFR, rpmRL, rpmRR;

    // physics body
    private PhysicsRigidBody rigidBody;

    private Vector3f collisionCenter;
    private Vector3f collisionHalfExtents;

    private List<MeshCollisionBox> meshCollisionBoxes = new ArrayList<>();
    private boolean collisionBoxesVisible = false;

    private Vector3f frontLeftPropeller;
    private Vector3f frontRightPropeller;
    private Vector3f rearLeftPropeller;
    private Vector3f rearRightPropeller;

    public float FL, FR, RL, RR; // throttle values for each motor (0.0 to 1.0)

    // acceleration tracking (computed from velocity deltas)
    private final Vector3f previousVelocity = new Vector3f();
    private final Vector3f acceleration = new Vector3f();

    // visual representation
    private DroneBody droneBody;
    private Model model;

    // control inputs (0.0 to 1.0)
    private float throttle = 0.0f;
    private float pitch = 0.0f;    // Forward/backward tilt
    private float roll = 0.0f;     // Left/right tilt
    private float yaw = 0.0f;      // Rotation around vertical axis

    // flight controller (PID stabilization by default; swap via setController)
    private FlightController controller = new DroneController();

    // autonomous waypoint patrol queue
    private static final float WAYPOINT_ARRIVAL_RADIUS = 3.0f;
    private List<Vector3f> waypointQueue = Collections.emptyList();
    private int waypointIndex = 0;

    private DroneBehavior behavior;

    public FlightController getController() {
        return controller;
    }

    public void setController(FlightController newController) {
        if (newController == null) {
            newController = new DroneController();
        }
        Vector3f prevTarget = controller != null ? controller.getTargetPosition() : null;
        boolean prevHold = controller != null && controller.isPositionHoldEnabled();
        this.controller = newController;
        if (prevTarget != null) {
            controller.setTargetPosition(prevTarget);
        }
        controller.setPositionHoldEnabled(prevHold);
    }

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
        model = droneBody; // DroneBody now extends Model

        // Get propeller positions from the model's rotor groups
        // TODO: fix the names of the meshes to just search for the cylender mesh instead
        // of relying on the group names. works fine at the moment
        frontLeftPropeller = model.getGroupPosition("rotors_front_left");
        frontRightPropeller = model.getGroupPosition("rotors_front_right");
        rearLeftPropeller = model.getGroupPosition("rotors_rear_left");
        rearRightPropeller = model.getGroupPosition("rotors_rear_right");

        logger.info("Propeller positions - FL: {}, FR: {}, RL: {}, RR: {}",
                frontLeftPropeller, frontRightPropeller, rearLeftPropeller, rearRightPropeller);

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
    // TODO: This function shouldn't be owned by drone. It should be a function of the model or renderer.
    public void setCollisionShapesVisible(boolean visible) {
        if (model == null || visible == collisionBoxesVisible) {
            return;
        }

        this.collisionBoxesVisible = visible;
        List<MeshData> meshForDeletion = new ArrayList<>();

        // Remove existing collision debug markers
        for (MeshData mesh : model.getMeshDataList()) {
            if (mesh.getMesh().getMeshName() != null && mesh.getMesh().getMeshName().startsWith("debug_collision_")) {
                meshForDeletion.add(mesh);
            }
        }
        meshForDeletion.forEach(model::remove);

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

        // Run stabilization controller — takes raw stick inputs + sensor state,
        // outputs per-motor values with PID corrections applied
        Quaternion orientation = rigidBody.getPhysicsRotation(null);
        Vector3f angularVelocity = rigidBody.getAngularVelocity(null);
        Vector3f position = rigidBody.getPhysicsLocation(null);
        Vector3f velocity = rigidBody.getLinearVelocity(null);

        // Compute acceleration: a = (v - v_prev) / dt
        if (deltaTime > 0f) {
            acceleration.set(
                    (velocity.x - previousVelocity.x) / deltaTime,
                    (velocity.y - previousVelocity.y) / deltaTime,
                    (velocity.z - previousVelocity.z) / deltaTime);
            previousVelocity.set(velocity);
        }

        controller.update(throttle, roll, pitch, yaw, orientation, angularVelocity,
                position, velocity, acceleration, deltaTime);

        FL = controller.getMotorFL();
        FR = controller.getMotorFR();
        RL = controller.getMotorRL();
        RR = controller.getMotorRR();

        // Motor command (0-1) → RPM → Thrust via propeller equation
        rpmFL = FL * MAX_MOTOR_RPM;
        rpmFR = FR * MAX_MOTOR_RPM;
        rpmRL = RL * MAX_MOTOR_RPM;
        rpmRR = RR * MAX_MOTOR_RPM;

        // T = Ct × ρ × n² × D⁴  (n in revs/sec)
        float nFL = rpmFL / 60f;
        float nFR = rpmFR / 60f;
        float nRL = rpmRL / 60f;
        float nRR = rpmRR / 60f;

        applyThrustAtPoint(frontLeftPropeller, THRUST_FACTOR * nFL * nFL);
        applyThrustAtPoint(frontRightPropeller, THRUST_FACTOR * nFR * nFR);
        applyThrustAtPoint(rearLeftPropeller, THRUST_FACTOR * nRL * nRL);
        applyThrustAtPoint(rearRightPropeller, THRUST_FACTOR * nRR * nRR);

        droneBody.updateModel(rpmFL, rpmFR, rpmRL, rpmRR, deltaTime);

        if (behavior != null) {
            behavior.update(this, deltaTime);
        }

        if (!waypointQueue.isEmpty()) {
            Vector3f target = waypointQueue.get(waypointIndex);
            if (position.distance(target) < WAYPOINT_ARRIVAL_RADIUS) {
                waypointIndex = (waypointIndex + 1) % waypointQueue.size();
                controller.setTargetPosition(waypointQueue.get(waypointIndex));
            }
        }
    }

    /**
     * Applies an upward thrust force at a local point on the drone. Converts
     * the local point to world space using the body's orientation, producing
     * both linear force and torque from the offset.
     */
    private void applyThrustAtPoint(Vector3f localPoint, float thrustForce) {
        if (localPoint == null || thrustForce == 0f) {
            return;
        }

        // Rotate local offset into world space
        Quaternion rot = rigidBody.getPhysicsRotation(null);
        Matrix3f rotMatrix = rot.toRotationMatrix();
        Vector3f worldOffset = rotMatrix.mult(localPoint, new Vector3f());

        // Thrust always acts along the body's local up axis
        Vector3f force = rotMatrix.mult(new Vector3f(0, thrustForce, 0), new Vector3f());

        rigidBody.applyForce(force, worldOffset);
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

    public Quaternion getOrientation() {
        if (rigidBody == null) {
            return new Quaternion();
        }
        return rigidBody.getPhysicsRotation(null);
    }

    public float getRpmFL() {
        return rpmFL;
    }

    public float getRpmFR() {
        return rpmFR;
    }

    public float getRpmRL() {
        return rpmRL;
    }

    public float getRpmRR() {
        return rpmRR;
    }

    // Gets the current linear acceleration 
    public Vector3f getAcceleration() {
        return acceleration;
    }

    public void setThrottle(float throttle) {
        this.throttle = Math.max(0, Math.min(1, throttle));
    }

    public float getThrottle() {
        return throttle;
    }

    public float getPitch() {
        return pitch;
    }

    public float getRoll() {
        return roll;
    }

    public float getYaw() {
        return yaw;
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

    /**
     * Set a world-space point the drone will fly toward (enables position
     * hold). Cancels existing momentum so the drone starts each new leg from
     * rest.
     */
    public void setTargetPosition(Vector3f target) {
        controller.setTargetPosition(target);

    }

    /**
     * Get the current target position, or null if none.
     */
    public Vector3f getTargetPosition() {
        return controller.getTargetPosition();
    }

    /**
     * Assigns a looping patrol route. The drone will cycle through
     * {@code waypoints} indefinitely, advancing to the next point whenever it
     * comes within {@value #WAYPOINT_ARRIVAL_RADIUS} metres of the current one.
     *
     * @param waypoints ordered list of world-space targets (must not be empty)
     * @param startIndex index into {@code waypoints} where the drone begins
     */
    public void setWaypointQueue(List<Vector3f> waypoints, int startIndex) {
        if (waypoints == null || waypoints.isEmpty()) {
            return;
        }
        this.waypointQueue = new ArrayList<>(waypoints);
        this.waypointIndex = ((startIndex % waypointQueue.size()) + waypointQueue.size()) % waypointQueue.size();
        controller.setTargetPosition(waypointQueue.get(waypointIndex));
    }

    /**
     * Removes the patrol route. The drone keeps its last target position.
     */
    public void clearWaypointQueue() {
        waypointQueue = Collections.emptyList();
        waypointIndex = 0;
    }

    public void setBehavior(DroneBehavior behavior) {
        this.behavior = behavior;
    }

    public DroneBehavior getBehavior() {
        return behavior;
    }

    /**
     * Enable / disable position hold without clearing the target.
     */
    public void setPositionHoldEnabled(boolean enabled) {
        // This function is currently faulty. It will inadvertently disable the autopilot (if it can be called like that)
        controller.setPositionHoldEnabled(enabled);
    }

    public boolean isPositionHoldEnabled() {
        return controller.isPositionHoldEnabled();
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
            previousVelocity.set(0, 0, 0);
            acceleration.set(0, 0, 0);
            waypointQueue = Collections.emptyList();
            waypointIndex = 0;
            controller.reset();
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
        model.addDebugMarker("prop_front_left", frontLeftPropeller,
                markerSize, new Vector3f(0.2f, 1.0f, 0.2f));
        model.addDebugMarker("prop_front_right", frontRightPropeller,
                markerSize, new Vector3f(0.2f, 0.8f, 0.2f));

        // Rear propellers - red tint
        model.addDebugMarker("prop_rear_left", rearLeftPropeller,
                markerSize, new Vector3f(1.0f, 0.2f, 0.2f));
        model.addDebugMarker("prop_rear_right", rearRightPropeller,
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
