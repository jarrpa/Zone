package com.team1816.core.states;

import com.google.inject.Singleton;
import com.team1816.core.configuration.Constants;
import com.team1816.core.configuration.FieldConfig;
import com.team1816.lib.Injector;
import com.team1816.lib.auto.Color;
import com.team1816.lib.auto.actions.TrajectoryAction;
import com.team1816.lib.freedomPath.FreedomPath;
import com.team1816.lib.subsystems.drive.SwerveDrive;
import com.team1816.lib.util.visionUtil.VisionPoint;
import com.team1816.season.auto.DynamicAutoScript2025;
import com.team1816.season.subsystems.AlgaeCatcher;
import com.team1816.season.subsystems.CoralArm;
import com.team1816.season.subsystems.Elevator;
import com.team1816.season.subsystems.Pneumatic;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.smartdashboard.*;
import org.photonvision.EstimatedRobotPose;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is responsible for logging the robot's actual states and estimated states.
 * Including superstructure and subsystem states.
 */

@Singleton
public class RobotState {

    /**
     * Odometry and field characterization
     */
    public final Field2d field = new Field2d();
    public Color allianceColor = Color.BLUE;
    public Pose2d fieldToVehicle = Constants.EmptyPose2d;
    public Pose2d driverRelativeFieldToVehicle = Constants.EmptyPose2d;
    public Pose2d extrapolatedFieldToVehicle = Constants.EmptyPose2d;
    public Pose2d target = Constants.fieldCenterPose;
    public ChassisSpeeds deltaVehicle = new ChassisSpeeds(); // velocities of vehicle
    public ChassisSpeeds calculatedVehicleAccel = new ChassisSpeeds(); // calculated acceleration of vehicle
    public Double[] triAxialAcceleration = new Double[]{0d, 0d, 0d};
    public boolean isPoseUpdated = true;
    public double vehicleToFloorProximityCentimeters = 0;
    public double drivetrainTemp = 0;
    public SwerveDrivePoseEstimator swerveEstimator =
            new SwerveDrivePoseEstimator(
                    SwerveDrive.swerveKinematics,
                    Constants.EmptyRotation2d,
                    new SwerveModulePosition[]{
                            new SwerveModulePosition(),
                            new SwerveModulePosition(),
                            new SwerveModulePosition(),
                            new SwerveModulePosition()
                    },
                    new Pose2d() //TODO figure out what to initialize this to
            );

    /**
     * Current Drive inputs and states
     */
    public double throttleInput = 0;
    public double strafeInput = 0;
    public double rotationInput = 0;
    public int robotcentricRequestAmount = 4; //this is here because robot on startup will activate all not pressed commands, so this counters it
    public double robotcentricThrottleInput = 0;
    public double robotcentricStrafeInput = 0;
    public double robotcentricRotationInput = 0;
    public double robotcentricInput = 0.3; //0 to 1

    /**
     * Rotating closed loop
     */

    public boolean rotatingClosedLoop = false;
    public double targetRotationRadians = 0;

    /**
     * Orchestrator states
     */

    //TODO add new subystem states here
    public AlgaeCatcher.ALGAE_CATCHER_INTAKE_STATE actualAlgaeCatcherIntakeState = AlgaeCatcher.ALGAE_CATCHER_INTAKE_STATE.STOP;
    public AlgaeCatcher.ALGAE_CATCHER_PIVOT_STATE actualAlgaeCatcherPivotState = AlgaeCatcher.ALGAE_CATCHER_PIVOT_STATE.STOW;
    public CoralArm.INTAKE_STATE actualCoralArmIntakeState = CoralArm.INTAKE_STATE.INTAKE;
    public CoralArm.PIVOT_STATE actualCoralArmPivotState = CoralArm.PIVOT_STATE.FEEDER;
    public Elevator.ELEVATOR_STATE actualElevatorState = Elevator.ELEVATOR_STATE.FEEDER;
    public Pneumatic.PNEUMATIC_STATE actualPneumaticState = Pneumatic.PNEUMATIC_STATE.OFF;

    public boolean isCoralBeamBreakTriggered = false;

    public boolean isElevatorInRange = false;

    public VisionPoint superlativeTarget = new VisionPoint();
    public List<VisionPoint> visibleTargets = new ArrayList<>();

    public final Mechanism2d elevatorAndCoralArmMech2d = new Mechanism2d(3, 3);
    public final MechanismRoot2d elevatorAndCoralArmMech2dRoot = elevatorAndCoralArmMech2d.getRoot("root", 1, 0);

    public final MechanismLigament2d elevatorMechArm = elevatorAndCoralArmMech2dRoot.append(new MechanismLigament2d("stand", 1, 90));
    public final double coralMechArmBaseAngle = 190;
    public final MechanismLigament2d coralMechArm = elevatorMechArm.append(new MechanismLigament2d("pivot", .7, coralMechArmBaseAngle));

    public final Mechanism2d algaeMech2d = new Mechanism2d(3,3);
    public final MechanismRoot2d getAlgaeCatcherMech2dRoot = algaeMech2d.getRoot("root", 1, 0);

    public final MechanismLigament2d algaeCatcherBase = getAlgaeCatcherMech2dRoot.append(new MechanismLigament2d("stand", 1, 90));
    public final double algaeBaseAngle = 190;
    public final MechanismLigament2d algaeCatcherPivot = algaeCatcherBase.append(new MechanismLigament2d("algaePivot", .7, algaeBaseAngle));

    /**
     * FreedomPathing state
     */

    public boolean isFreedomPathing = false;
    public boolean printFreedomPathing = false;  //Change this one to see the obstacle boundaries //As of 2/8/2025, does nothing because of commented code in outputToSmartDashboard()
    public boolean printFreedomPathFieldTest = false;
    public Trajectory freedomPathTrajectory = null;
    public ArrayList<Trajectory> freedomPathTrajectoryPossibilities = new ArrayList<>();
    public boolean freedomPathTrajectoryChanged = false;
    public boolean freedomPathTrajectoryPossibilitiesChanged = false;
    public ArrayList<Pose2d> freedomPathCollisionStarts = new ArrayList<>();
    public ArrayList<Pose2d> freedomPathCollisionEnds = new ArrayList<>();
    public ArrayList<Pose2d> freedomPathWaypoints = new ArrayList<>();
    public ArrayList<Pose2d> freedomPathWaypointsSuccess = new ArrayList<>();
    public ArrayList<Pose2d> freedomPathWaypointsFail = new ArrayList<>();
    public int freedomPathMaxBranches = 0;
    public ArrayList<Pose2d> freedomPathInputWaypoints = new ArrayList<>();
    public double robotVelocity = 0;
    public double freedomPathBeforeTime = 0;
    public double freedomPathCancelBufferMilli = 500;
    public ChassisSpeeds robotChassis = new ChassisSpeeds();

    /**
     * DynamicAuto2025
     */
    public boolean dAutoChanged = false;
    public boolean dIsAutoDynamic = false;
    public HashMap<String, Pose2d> dAllDynamicPoints;
    public Pose2d dStartPose;
    public ArrayList<TrajectoryAction> dAutoTrajectoryActions;
    public ArrayList<DynamicAutoScript2025.REEF_LEVEL> dCurrentCoralPlacementChoices;

    /**
     * Pigeon state
     */

    public double[] gyroPos = new double[3];

    /**
     * Initializes RobotState and field
     */
    public RobotState() {
        resetPosition();
        FieldConfig.setupField(field);
    }

    /**
     * Vision Pose Stuff
     */
    public double lastEstTimestamp = 0;
    public final Matrix<N3, N1> kSingleTagStdDevs = VecBuilder.fill(4, 4, 8);
    public final Matrix<N3, N1> kMultiTagStdDevs = VecBuilder.fill(0.5, 0.5, 1);
    public EstimatedRobotPose currentVisionEstimatedPose;
    public boolean currentCamFind;

    /**
     * Resets drivetrain position to a specified pose of drivetrain
     *
     * @param initial_field_to_vehicle
     */
    public synchronized void resetPosition(Pose2d initial_field_to_vehicle) {
        fieldToVehicle = initial_field_to_vehicle;
    }

    /**
     * Resets the drivetrain to its default "zero" pose
     *
     * @see Constants
     */
    public synchronized void resetPosition() {
        resetPosition(Constants.kDefaultZeroingPose);
    }

    /**
     * Resets all values stored in RobotState
     */
    public synchronized void resetAllStates() {
        deltaVehicle = new ChassisSpeeds();
        calculatedVehicleAccel = new ChassisSpeeds();
        triAxialAcceleration = new Double[]{0d, 0d, 0d};

        // TODO: Insert any subsystem state set up here.
        actualAlgaeCatcherIntakeState = AlgaeCatcher.ALGAE_CATCHER_INTAKE_STATE.STOP;
        actualAlgaeCatcherPivotState = AlgaeCatcher.ALGAE_CATCHER_PIVOT_STATE.STOW;
        actualElevatorState = Elevator.ELEVATOR_STATE.FEEDER;
        actualPneumaticState = Pneumatic.PNEUMATIC_STATE.OFF;

        isPoseUpdated = true;
        superlativeTarget = new VisionPoint();
        visibleTargets = new ArrayList<>();
        drivetrainTemp = 0;
        vehicleToFloorProximityCentimeters = 0;
    }

    /**
     * Returns rotation of the camera with respect to the field
     *
     * @return Rotation2d
     * @see Orchestrator#calculateSingleTargetTranslation(VisionPoint) ()
     */
    public Rotation2d getLatestFieldToCamera() {
        return fieldToVehicle.getRotation().plus(Constants.kCameraMountingOffset.getRotation());
    }

    /**
     * Locks robot rotation to a specific angle, then terminates rotation once angle is reached
     *
     * @param targetRotationRadians
     * @return
     */
    public boolean setRobotRotatingClosedLoop(double targetRotationRadians){
        this.targetRotationRadians = targetRotationRadians;
        rotatingClosedLoop = true;

        return fieldToVehicle.getRotation().getRadians() == targetRotationRadians;
    }

    /**
     * Returns the estimated / calculated acceleration of the robot based on sensor readings
     *
     * @return ChassisSpeeds
     */
    public synchronized ChassisSpeeds getCalculatedAccel() {
        return calculatedVehicleAccel;
    }

    /**
     * Outputs real-time telemetry data to Shuffleboard / SmartDashboard
     */
    public synchronized void outputToSmartDashboard() {
        field.setRobotPose(fieldToVehicle);

        if (printFreedomPathing) {
            FreedomPath freedomPath = Injector.get(FreedomPath.class);
            if (freedomPath.fieldMap != null && freedomPath.fieldMap.outputToSmartDashboardChanged) {
                ArrayList<Pose2d> obstaclesExpanded = new ArrayList<>();

                for (int i = 0; i < freedomPath.fieldMap.getCurrentMap().getMapX(); i++) {
                    for (int i2 = 0; i2 < freedomPath.fieldMap.getCurrentMap().getMapY(); i2++) {
                        if (freedomPath.fieldMap.getCurrentMap().checkPixelHasObjectOrOffMap(i, i2)) {
                            obstaclesExpanded.add(new Pose2d(new Translation2d(i / freedomPath.mapResolution1DPerMeter, i2 / freedomPath.mapResolution1DPerMeter), new Rotation2d()));
                        }
                    }
                }

                field.getObject("ExpandedObstacles").setPoses(obstaclesExpanded);

                ArrayList<Pose2d> obstacles = new ArrayList<>();

                for (int i = 0; i < freedomPath.fieldMap.getCurrentMap().getMapX(); i++) {
                    for (int i2 = 0; i2 < freedomPath.fieldMap.getCurrentMap().getMapY(); i2++) {
                        if (freedomPath.fieldMap.getStableMapCheckPixelHasObjectOrOffMap(i, i2)) {
                            obstacles.add(new Pose2d(new Translation2d(i / freedomPath.mapResolution1DPerMeter, i2 /freedomPath.mapResolution1DPerMeter), new Rotation2d()));
                        }
                    }
                }

                field.getObject("Obstacles").setPoses(obstacles);

                freedomPath.fieldMap.outputToSmartDashboardChanged = false;
            }

            if(freedomPathTrajectoryPossibilitiesChanged) {
                for (int i = 0; i < freedomPathTrajectoryPossibilities.size(); i++) {
                    if (freedomPathTrajectoryPossibilities.get(i) != null) {
                        field.getObject("FreedomPathTrajectory: " + i).setTrajectory(freedomPathTrajectoryPossibilities.get(i));
                    }
                }
                freedomPathMaxBranches = Math.max(freedomPathTrajectoryPossibilities.size(), freedomPathMaxBranches);
                freedomPathTrajectoryPossibilitiesChanged = false;
            }

            field.getObject("StartCollisionPoints").setPoses(freedomPathCollisionStarts);
            field.getObject("EndCollisionPoints").setPoses(freedomPathCollisionEnds);
            field.getObject("FreedomPathWaypoints").setPoses(freedomPathWaypoints);
        }

        if (freedomPathTrajectoryChanged && printFreedomPathing) {
            if(freedomPathTrajectory != null){
                for (int i = 0; i < freedomPathMaxBranches; i++) {
                    field.getObject("FreedomPathTrajectory: " + i).close();
                }
                field.getObject("FreedomPathTrajectory").setTrajectory(freedomPathTrajectory);
            } else
                field.getObject("FreedomPathTrajectory").setPoses(List.of(new Pose2d(new Translation2d(-1, -1), new Rotation2d())));
            freedomPathTrajectoryChanged = false;
        }

        if(printFreedomPathFieldTest) {
            field.getObject("FreedomPathSuccessfulPoints").setPoses(freedomPathWaypointsSuccess);
            field.getObject("FreedomPathFailPoints").setPoses(freedomPathWaypointsFail);
        }

        SmartDashboard.putData("Elevator+CoralArm", elevatorAndCoralArmMech2d);
        SmartDashboard.putData("AlgaeCatcher", algaeMech2d);
//        System.out.println(fieldToVehicle);

        if (RobotBase.isSimulation()) {
            // TODO: Display any stats here

            // e.g.
            SmartDashboard.putNumber(
                    "Path_to_Subsystem/Value",
                    02390293.23
            );
        }
    }
}
