package com.paperpiper.drone;

import com.paperpiper.render.Model;

// Represents the body of a drone. Should contain visual and physical properties.
// eg mesh, rotor spinning, lights, trails, etc
// This could be child class of model.
public class DroneBody {

    private final Model model;

    // Constructor
    public DroneBody() {
        model = new Model("drone");
        model.loadModel("src/main/resources/Models/drone_proper.glb"); 

        setupRotorArrangement();


    }

    private void setupRotorArrangement() {
        // The model doesn't provide UV coordinates. waste of time honestly
        // int textureId = model.loadTexture("src/main/resources/Textures/checkboard.png");
        changeColor("cf_body.001", 0.4f, 0.5f, 0.3f);
        changeColor("cw_prop", 0.4f, 0.3f, 0.3f);

        // transformations to position/scale the model correctly
        // the current arm that has stuff is left front.
        model.createGroup("rotors_front_left");
        model.createGroup("rotors_front_right");
        model.createGroup("rotors_rear_left");
        model.createGroup("rotors_rear_right");

        model.addMeshToGroup(new String[]{"cw_prop", "motor_mount", "Cylinder"}, "rotors_front_left");

        // Front-right: mirror across X-axis (left becomes right)
        // Rotate 180° around Y to flip propeller direction (CW -> CCW appearance)
        // Rear-left: mirror across Z-axis (front becomes back)
        // Rotate 180° around Y to flip propeller direction (CW -> CCW appearance)
        model.copyGroup("rotors_front_left", "rotors_rear_left");
        model.flipGroup("rotors_rear_left", true, false, false);
        model.rotateGroup("rotors_rear_left", 0, 90, 0);

        model.copyGroup("rotors_front_left", "rotors_front_right");
        model.flipGroup("rotors_front_right", false, false, true );
        model.rotateGroup("rotors_front_right", 0, 270, 0);

        // Rear-right: mirror across both X and Z axes (diagonal)
        // No Y rotation needed - keeps same CW direction as front-left
        model.copyGroup("rotors_front_left", "rotors_rear_right");
        model.flipGroup("rotors_rear_right", true, false, true);
        model.rotateGroup("rotors_rear_right", 0, 180, 0);
    }

    private void textureMesh(String textureName, String materialName, int textureId) {
        model.textureMesh(textureName, materialName, textureId);
    }

    private void changeColor(String meshName, float r, float g, float b) {
        model.changeColor(meshName, r, g, b);
    }

    public Model getModel() {
        return model;
    }
}
