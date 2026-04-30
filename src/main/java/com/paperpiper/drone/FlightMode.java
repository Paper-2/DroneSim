package com.paperpiper.drone;


public enum FlightMode {
    MANUAL, // just controller input.  no stabilization.
    STABILIZED, // controller input + attitude stabilization.
    ALTITUDE_HOLD, // stabilized + vertical-velocity loop; horizontal still manual.
    POSITION_HOLD, // full cascade: position  velocity  attitude  rate.
    EMERGENCY_LAND // slowly descend to the ground, ignoring all other inputs.
}
