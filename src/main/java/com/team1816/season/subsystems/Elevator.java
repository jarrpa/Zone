package com.team1816.season.subsystems;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.pathplanner.lib.auto.NamedCommands;
import com.team1816.core.states.RobotState;
import com.team1816.lib.Infrastructure;
import com.team1816.lib.hardware.components.motor.GhostMotor;
import com.team1816.lib.hardware.components.motor.IGreenMotor;
import com.team1816.lib.hardware.components.motor.configurations.GreenControlMode;
import com.team1816.lib.subsystems.Subsystem;
import com.team1816.lib.util.logUtil.GreenLogger;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Commands;

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

    /**
     * States
     */

    private ELEVATOR_STATE desiredElevatorState = ELEVATOR_STATE.FEEDER;

    private boolean elevatorOutputsChanged = false;
    private boolean offsetHasBeenApplied = false;

    private double elevatorCurrentDraw;

    private double desiredElevatorPosition = 0;
    private double actualElevatorPosition = 0;

    private double elevatorMotorRotationsPerUnit = factory.getConstant(NAME, "elevatorMotorRotationsPerUnit", 1);

    /**
     * Constants
     */

    private double elevatorFeederPosition = factory.getConstant(NAME, "elevatorFeederPosition", 1.0);
    private double elevatorL2CoralPosition = factory.getConstant(NAME, "elevatorL2CoralPosition", 1.0);
    private double elevatorL3CoralPosition = factory.getConstant(NAME, "elevatorL3CoralPosition", 1.0);
    private double elevatorL4Position = factory.getConstant(NAME, "elevatorL4Position", 1.0);
    private double elevatorL2AlgaePosition = factory.getConstant(NAME, "elevatorL2AlgaePosition", 1.0);
    private double elevatorL3AlgaePosition = factory.getConstant(NAME, "elevatorL3AlgaePosition", 1.0);

//    private final boolean opposeLeaderDirection = ((int) factory.getConstant(NAME, "invertFollowerMotor", 0)) == 1;

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

        elevatorMotor.selectPIDSlot(0);

        if (RobotBase.isSimulation()) {
            elevatorMotor.setMotionProfileMaxVelocity(5);
            elevatorMotor.setMotionProfileMaxAcceleration(10);
            ((GhostMotor) elevatorMotor).setMaxVelRotationsPerSec(1);
        }
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

        robotState.elevatorMechArm.setLength(elevatorMotor.getSensorPosition() / elevatorMotorRotationsPerUnit);

        if (robotState.actualElevatorState != desiredElevatorState) {
            System.out.println("changed");
            robotState.actualElevatorState = desiredElevatorState;
            elevatorOutputsChanged = true;
        }
    }

    /**
     * Writes outputs to shooter motors
     *
     * @see Subsystem#writeToHardware()
     */
    @Override
    public void writeToHardware() {
        if (elevatorOutputsChanged || offsetHasBeenApplied) {
            elevatorOutputsChanged = false;
            offsetHasBeenApplied = false;

            desiredElevatorPosition = getElevatorPosition(desiredElevatorState);
//            System.out.println("Elevator state: "+desiredElevatorState+" Position: "+desiredElevatorPosition);
            if (robotState.actualRampState == Ramp.RAMP_STATE.L1_FEEDER)
                elevatorOutputsChanged = true;
            else
                elevatorMotor.set(GreenControlMode.MOTION_MAGIC_EXPO, MathUtil.clamp(desiredElevatorPosition, 0, 67));
            SmartDashboard.putString("Elevator desired state", String.valueOf(desiredElevatorState));
            SmartDashboard.putNumber("Elevator desired position", desiredElevatorPosition);}
    }

    public void offsetElevator(double offsetAmount){
        switch (desiredElevatorState) {
            case L2_CORAL -> elevatorL2CoralPosition += offsetAmount;
            case L3_CORAL -> elevatorL3CoralPosition += offsetAmount;
            case L4 -> elevatorL4Position += offsetAmount;
            case FEEDER -> elevatorFeederPosition += offsetAmount;
            case L2_ALGAE -> elevatorL2AlgaePosition += offsetAmount;
            case L3_ALGAE -> elevatorL3AlgaePosition += offsetAmount;
        }
        offsetHasBeenApplied = true;
        GreenLogger.log("Elevator " + desiredElevatorState + " position set to " + getElevatorPosition(desiredElevatorState));
    }

    public boolean isElevatorInRange(){
        return Math.abs(elevatorMotor.getSensorPosition() - desiredElevatorPosition) < 2;
    }

    @Override
    public void zeroSensors() {
        elevatorMotor.setSensorPosition(0);
    }

    @Override
    public void stop() {

    }

    /**
     * Registers commands for:
     * <ul>
     *     <li>elevator l1</li>
     *     <li>elevator l2</li>
     *     <li>elevator l3</li>
     *     <li>elevator l4</li>
     *     <li>elevator feeder</li>
     * </ul>
     */
    @Override
    public void implementNamedCommands() {
        for (ELEVATOR_STATE elevatorState : ELEVATOR_STATE.values()) {
            NamedCommands.registerCommand(NAME + " " + elevatorState.toString().toLowerCase(),
                    Commands.runOnce(() -> setDesiredState(elevatorState)).alongWith(Commands.waitUntil(this::isElevatorInRange)));
        }
    }

    public void setBraking(boolean braking) {
        elevatorMotor.setNeutralMode(braking ? NeutralMode.Brake : NeutralMode.Coast);
    }

    @Override
    public boolean testSubsystem() {
        //TODO eventually.
        return false;
    }

    private double getElevatorPosition(ELEVATOR_STATE elevatorState) {
        return switch (elevatorState) {
            case L2_CORAL -> elevatorL2CoralPosition;
            case L3_CORAL -> elevatorL3CoralPosition;
            case L4 -> elevatorL4Position;
            case FEEDER -> elevatorFeederPosition;
            case L2_ALGAE -> elevatorL2AlgaePosition;
            case L3_ALGAE -> elevatorL3AlgaePosition;
        };
    }

    /**
     * Returns the desired elevator state
     *
     * @return desired elevator state
     */
    public ELEVATOR_STATE getDesiredElevatorState() {return desiredElevatorState;}
    public ELEVATOR_STATE getDesisetredElevatorState() {return desiredElevatorState;}

    /**
     * Elevator enum
     */
    public enum ELEVATOR_STATE {
        FEEDER,
        L2_CORAL,
        L3_CORAL,
        L4,
        L2_ALGAE,
        L3_ALGAE
    }
}
