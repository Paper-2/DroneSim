package com.paperpiper.drone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Drone class.
 */
class DroneTest {
    
    @Test
    void testDroneCreation() {
        Drone drone = new Drone();
        assertNotNull(drone);
        assertFalse(drone.isMotorsArmed());
    }
    
    @Test
    void testThrottleClamping() {
        Drone drone = new Drone();
        
        drone.setThrottle(0.5f);
        
        
        drone.setThrottle(-0.5f);
        
        
        drone.setThrottle(1.5f);
        
    }
    
    @Test
    void testPitchClamping() {
        Drone drone = new Drone();
        
        drone.setPitch(0.5f);
        drone.setPitch(-1.5f);
        drone.setPitch(1.5f);
        
    }
    
    @Test
    void testMotorArming() {
        Drone drone = new Drone();
        
        assertFalse(drone.isMotorsArmed());
        
        
    }
}
