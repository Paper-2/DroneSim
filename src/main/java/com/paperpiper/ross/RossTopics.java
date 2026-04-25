package com.paperpiper.ross;

/**
 * Standard ROS2 topic names used by the DroneSim ROSS protocol.
 */
public final class RossTopics {

    private RossTopics() {
    }

    /**
     * Telemetry pose + velocity for a single drone.
     */
    public static final String TELEMETRY = "/drone/telemetry";

    /**
     * Raw camera image stream.
     */
    public static final String CAMERA_IMAGE = "/drone/camera/image_raw";

    /**
     * Manual velocity command (Twist).
     */
    public static final String CMD_VEL = "/drone/cmd_vel";

    /**
     * Builds a drone-specific topic: e.g. /drone/drone-1/telemetry
     */
    public static String forDrone(String droneId, String baseTopic) {
        return "/drone/" + droneId + baseTopic.substring("/drone".length());
    }
}
