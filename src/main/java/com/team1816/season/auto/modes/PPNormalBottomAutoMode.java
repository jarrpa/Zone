package com.team1816.season.auto.modes;

import com.team1816.lib.auto.AutoModeEndedException;
import com.team1816.lib.auto.actions.PathPlannerAction;
import com.team1816.lib.auto.modes.AutoMode;
import edu.wpi.first.math.geometry.Pose2d;

/**
 * Uses PathPlanner "Normal Bottom Auto"
 */
public class PPNormalBottomAutoMode extends AutoMode {
    private final PathPlannerAction action = new PathPlannerAction("Copy of Normal Bottom Auto", PathPlannerAction.ActionType.AUTO);

    @Override
    protected void routine() throws AutoModeEndedException {
        runAction(action);
    }

    @Override
    public Pose2d getInitialPose() {
        return action.getPathInitialPose();
    }
}
