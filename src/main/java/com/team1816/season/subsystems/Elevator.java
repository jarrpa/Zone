package com.team1816.season.subsystems;

import com.ctre.phoenix.Util;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.team1816.core.configuration.Constants;
import com.team1816.core.states.RobotState;
import com.team1816.lib.Infrastructure;
import com.team1816.lib.hardware.components.motor.GhostMotor;
import com.team1816.lib.hardware.components.motor.IGreenMotor;
import com.team1816.lib.hardware.components.motor.LazyTalonFX;
import com.team1816.lib.hardware.components.motor.configurations.GreenControlMode;
import com.team1816.lib.subsystems.Subsystem;
import com.team1816.lib.util.logUtil.GreenLogger;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.util.datalog.BooleanLogEntry;
import edu.wpi.first.util.datalog.DoubleLogEntry;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Optional;

@Singleton
public class Elevator extends Subsystem {

    /**
     * Name
     */
    private static final String NAME = "elevator";


    /**
     * Components
     */
    private final IGreenMotor elevatorMotor;
    private final IGreenMotor elevatorFollowMotor;

    /**
     * States
     */

    private ELEVATOR_STATE desiredElevatorState = ELEVATOR_STATE.GROUND;

    private boolean elevatorOutputsChanged = false;

    private double elevatorCurrentDraw;

    private double desiredElevatorPosition = 0;
    private double actualElevatorPosition = 0;
    private double actualElevatorDegrees = 0;



    /**
     * Constants
     */

    private final double elevatorGroundPosition = factory.getConstant(NAME, "elevatorGroundPosition", 1.0);
    private final double elevatorL1Position = factory.getConstant(NAME, "elevatorL1Position", 1.0);
    private final double elevatorL2Position = factory.getConstant(NAME, "elevatorL2Position", 1.0);
    private final double elevatorL3Position = factory.getConstant(NAME, "elevatorL3Position", 1.0);
    private final double elevatorL4Position = factory.getConstant(NAME, "elevatorL4Position", 1.0);

    private final boolean opposeLeaderDirection = ((int) factory.getConstant(NAME, "invertFollowerMotor", 0)) == 1;

    /**
     * Base constructor needed to instantiate a shooter
     *
     * @param inf Infrastructure
     * @param rs  RobotState
     */
    @Inject
    public Elevator(Infrastructure inf, RobotState rs) {
        super(NAME, inf, rs);
        elevatorMotor = factory.getMotor(NAME, "elevatorMotor");
        elevatorFollowMotor = factory.getFollowerMotor(NAME, "elevatorFollowMotor", elevatorMotor, opposeLeaderDirection);

        elevatorMotor.selectPIDSlot(2);

        if (RobotBase.isSimulation()) {
            elevatorMotor.setMotionProfileMaxVelocity(12 / 0.05);
            elevatorMotor.setMotionProfileMaxAcceleration(12 / 0.08);
            ((GhostMotor) elevatorMotor).setMaxVelRotationsPerSec(240);
        }
    }

    /**
     * Sets the desired state of the elevator
     *
     * @param desiredElevatorState ELEVATOR_STATE
     */
    public void setDesiredElevatorState(ELEVATOR_STATE desiredElevatorState) {
        this.desiredElevatorState = desiredElevatorState;
        elevatorOutputsChanged = true;
    }

    /**
     * Sets the desired state of the elevator
     *
     * @param desiredElevatorState ELEVATOR_STATE
     */
    public void setDesiredState(ELEVATOR_STATE desiredElevatorState) {
        this.desiredElevatorState = desiredElevatorState;

        elevatorOutputsChanged = true;
    }

    /**
     * Reads actual outputs from shooter motors
     *
     * @see Subsystem#readFromHardware()
     */
    @Override
    public void readFromHardware() {
        actualElevatorPosition = elevatorMotor.getSensorPosition();

        elevatorCurrentDraw = elevatorMotor.getMotorOutputCurrent();

        if (robotState.actualElevatorState != desiredElevatorState) {
            robotState.actualElevatorState = desiredElevatorState;
        }
    }

    /**
     * Writes outputs to shooter motors
     *
     * @see Subsystem#writeToHardware()
     */
    @Override
    public void writeToHardware() {
        if (elevatorOutputsChanged) {
            elevatorOutputsChanged = false;
            switch (desiredElevatorState) {
                case GROUND -> {
                    desiredElevatorPosition = elevatorGroundPosition;
                }
                case L1 -> {
                    desiredElevatorPosition = elevatorL1Position;
                }
                case L2 -> {
                    desiredElevatorPosition = elevatorL2Position;
                }
                case L3 -> {
                    desiredElevatorPosition = elevatorL3Position;
                }
                case L4 -> {
                    desiredElevatorPosition = elevatorL4Position;
                }
            }
            elevatorMotor.set(GreenControlMode.MOTION_MAGIC_EXPO, MathUtil.clamp(desiredElevatorPosition, 1.5, 35));
        }
    }

    public double getActualPivotPosition () {
        return elevatorMotor.getSensorPosition();
    }

    @Override
    public void zeroSensors() {
        elevatorMotor.setSensorPosition(0, 50);
    }

    @Override
    public void stop() {

    }

    public void setBraking(boolean braking) {
        elevatorMotor.setNeutralMode(braking ? NeutralMode.Brake : NeutralMode.Coast);
    }

    @Override
    public boolean testSubsystem() {
        //TODO eventually.
        return false;
    }

    /**
     * Returns the desired elevator state
     *
     * @return desired elevator state
     */
    public ELEVATOR_STATE getDesiredElevatorState() {
        return desiredElevatorState;
    }

    /**
     * Elevator enum
     */
    public enum ELEVATOR_STATE {
        GROUND,
        L1,
        L2,
        L3,
        L4
    }
}
