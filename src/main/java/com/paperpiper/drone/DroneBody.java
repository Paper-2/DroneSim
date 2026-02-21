package com.paperpiper.drone;

import com.paperpiper.render.Model;

// Represents the body of a drone. Should contain visual and physical properties.
// eg mesh, rotor spinning, lights, trails, etc
public class DroneBody extends Model {

    // Constructor
    public DroneBody() {
        super("drone");
        loadModel("src/main/resources/Models/drone_proper.glb");

        setupRotorArrangement();
    }

    private void setupRotorArrangement() {
        // The model doesn't provide UV coordinates. waste of time honestly
        // int textureId = loadTexture("src/main/resources/Textures/checkerboard.png");
        changeColor("cf_body.001", 0.4f, 0.5f, 0.3f);
        changeColor("cw_prop", 0.4f, 0.3f, 0.3f);

        // transformations to position/scale the model correctly
        // the current arm that has stuff is left front.
        createGroup("rotors_front_left");
        createGroup("rotors_front_right");
        createGroup("rotors_rear_left");
        createGroup("rotors_rear_right");

        addMeshToGroup(new String[]{"cw_prop", "motor_mount", "Cylinder"}, "rotors_front_left");

        // Front-right: mirror across X-axis (left becomes right)
        // Rotate 180° around Y to flip propeller direction (CW -> CCW appearance)
        // Rear-left: mirror across Z-axis (front becomes back)
        // Rotate 180° around Y to flip propeller direction (CW -> CCW appearance)
        copyGroup("rotors_front_left", "rotors_rear_left");
        flipGroup("rotors_rear_left", true, false, false);
        rotateGroup("rotors_rear_left", 0, 90, 0);

        copyGroup("rotors_front_left", "rotors_front_right");
        flipGroup("rotors_front_right", false, false, true);
        rotateGroup("rotors_front_right", 0, 270, 0);

        // Rear-right: mirror across both X and Z axes (diagonal)
        // No Y rotation needed - keeps same CW direction as front-left
        copyGroup("rotors_front_left", "rotors_rear_right");
        flipGroup("rotors_rear_right", true, false, true);
        rotateGroup("rotors_rear_right", 0, 180, 0);
    }

    public void updateModel(float FL, float FR, float RL, float RR, float deltaTime) {
        // Rotate the propellers based on time and throttle
        // This is a simple approximation. You could make it more complex by factoring in actual RPM, acceleration, etc.
        float RPM = 360.0f * deltaTime * 1.5f; // degrees per second at full throttle
        
        

        rotateMesh("cw_prop", 0, FL * RPM, 0);
        rotateMesh("rotors_front_right_cw_prop", 0, FR * RPM , 0);
        rotateMesh("rotors_rear_left_cw_prop", 0, RL * RPM, 0);
        rotateMesh("rotors_rear_right_cw_prop", 0, RR * RPM, 0);
    }
}
