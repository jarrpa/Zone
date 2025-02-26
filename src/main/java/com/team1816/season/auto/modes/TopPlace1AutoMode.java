package com.team1816.season.auto.modes;

import com.team1816.lib.auto.AutoModeEndedException;
import com.team1816.lib.auto.Color;
import com.team1816.lib.auto.actions.ParallelAction;
import com.team1816.lib.auto.actions.SeriesAction;
import com.team1816.lib.auto.actions.TrajectoryAction;
import com.team1816.lib.auto.actions.WaitAction;
import com.team1816.lib.auto.modes.AutoMode;
import com.team1816.season.auto.actions.*;
import com.team1816.season.auto.path.TopToSideOne;
import com.team1816.season.subsystems.CoralArm;
import com.team1816.season.subsystems.Elevator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;

import java.util.List;

public class TopPlace1AutoMode extends AutoMode {
    public TopPlace1AutoMode(Color color){
        super(
                List.of(
                        new TrajectoryAction(
                                new TopToSideOne(robotState.allianceColor)
                        )
                )
        );
    }
    @Override
    protected void routine() throws AutoModeEndedException {
        runAction(
                new SeriesAction(
                        new ParallelAction(
                                new DelayedElevatorAction(Elevator.ELEVATOR_STATE.L4, 2),
                                trajectoryActions.get(0)
                        ),
                        new WaitForCoralPivotPosition(CoralArm.PIVOT_STATE.L4),
                        new WaitAction(0.5),
                        new OuttakeCoralSeriesAction()
                )
        );
    }
}
