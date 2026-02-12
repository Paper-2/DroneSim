package com.paperpiper.physics;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;

public class PhysicsWorld {

    private static final Logger logger = LoggerFactory.getLogger(PhysicsWorld.class);

    private PhysicsSpace physicsSpace;
    private List<PhysicsRigidBody> bodies;

    // Physics constants
    private static final float GRAVITY = -9.81f; // m/s^2

    // Static initializer to load native library (cross-platform)
    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();

            String libName;
            String ext;
            if (os.contains("win")) {
                ext = ".dll";
                libName = "bulletjme";
            } else if (os.contains("mac")) {
                ext = ".dylib";
                libName = "libbulletjme";
            } else {
                ext = ".so";
                libName = "libbulletjme";
            }

            // Map arch to libbulletjme naming convention
            String archDir = arch.contains("64") ? (arch.contains("aarch") ? "arm64" : "x86_64") : "x86";
            String osDir = os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "linux";

            String resourcePath = "/natives/" + osDir + "/" + archDir + "/" + libName + ext;
            String flatResourcePath = "/natives/" + libName + ext;

            java.io.InputStream in = PhysicsWorld.class.getResourceAsStream(resourcePath);
            if (in == null) {
                // Try flat path
                in = PhysicsWorld.class.getResourceAsStream(flatResourcePath);
                resourcePath = flatResourcePath;
            }
            if (in != null) {
                java.nio.file.Path tempLib = java.nio.file.Files.createTempFile(libName, ext);
                java.nio.file.Files.copy(in, tempLib, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                in.close();
                System.load(tempLib.toAbsolutePath().toString());
                tempLib.toFile().deleteOnExit();
                logger.info("Bullet physics native library loaded from resources: {}", resourcePath);
            } else {
                // Fallback to build directory (try structured path first, then flat path)
                java.io.File nativeLib = new java.io.File("build/resources/main/natives/" + osDir + "/" + archDir + "/" + libName + ext);
                if (!nativeLib.exists()) {
                    // Try flat path (current gradle task downloads here)
                    nativeLib = new java.io.File("build/resources/main/natives/" + libName + ext);
                }
                if (nativeLib.exists()) {
                    System.load(nativeLib.getAbsolutePath());
                    logger.info("Bullet physics native library loaded from build directory: {}", nativeLib.getPath());
                } else {
                    throw new RuntimeException("Native library not found at " + resourcePath + ". Run 'gradlew downloadBulletNatives'.");
                }
            }
        } catch (Exception e) {
            logger.error("Failed to load Bullet physics native library", e);
            throw new RuntimeException("Failed to load native library", e);
        }
    }

    public PhysicsWorld() {
        bodies = new ArrayList<>();
    }

    public void init() {
        logger.info("Initializing Bullet physics world...");

        physicsSpace = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);

        physicsSpace.setGravity(new Vector3f(0, GRAVITY, 0));

        physicsSpace.setAccuracy(1f / 60f); // Update at 60 Hz
        physicsSpace.setMaxSubSteps(4);

        logger.info("Physics world initialized with gravity: {}", GRAVITY);
    }

    public void stepSimulation(float deltaTime) {
        physicsSpace.update(deltaTime);
    }

    public void addRigidBody(PhysicsRigidBody body) {
        physicsSpace.addCollisionObject(body);
        bodies.add(body);
    }

    public void removeRigidBody(PhysicsRigidBody body) {
        physicsSpace.removeCollisionObject(body);
        bodies.remove(body);
    }

    public PhysicsRigidBody createGroundPlane() {
        Plane groundPlane = new Plane(new Vector3f(0, 1, 0), 0);
        PlaneCollisionShape groundShape = new PlaneCollisionShape(groundPlane);

        PhysicsRigidBody ground = new PhysicsRigidBody(groundShape, 0f); // mass of 0 makes static a static object
        ground.setFriction(0.8f);
        ground.setRestitution(0.2f);

        addRigidBody(ground);
        logger.info("Ground plane created");

        return ground;
    }

    public PhysicsRigidBody createBox(Vector3f halfExtents, float mass, Vector3f position) {
        BoxCollisionShape boxShape = new BoxCollisionShape(halfExtents);

        PhysicsRigidBody box = new PhysicsRigidBody(boxShape, mass);
        box.setPhysicsLocation(position);
        box.setFriction(0.5f);
        box.setRestitution(0.3f);

        addRigidBody(box);

        return box;
    }

    /**
     * Create a rigid body with custom shape TODO: add overload to load glt
     * models
     */
    public PhysicsRigidBody createRigidBody(CollisionShape shape, float mass, Vector3f position) {
        PhysicsRigidBody body = new PhysicsRigidBody(shape, mass);
        body.setPhysicsLocation(position);

        addRigidBody(body);

        return body;
    }

    // Get physics space
    public PhysicsSpace getPhysicsSpace() {
        return physicsSpace;
    }

    // Get all rigid bodies
    public List<PhysicsRigidBody> getBodies() {
        return bodies;
    }

    // Set gravity
    public void setGravity(float gravity) {
        physicsSpace.setGravity(new Vector3f(0, gravity, 0));
    }

    // Cleanup physics resources
    public void cleanup() {
        logger.info("Cleaning up physics world...");

        // Remove all bodies
        for (PhysicsRigidBody body : bodies) {
            physicsSpace.removeCollisionObject(body);
        }
        bodies.clear();

        // Destroy physics space
        if (physicsSpace != null) {
            physicsSpace.destroy();
            physicsSpace = null;
        }
    }
}
