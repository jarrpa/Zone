package com.team1816.season.auto.path;

import com.team1816.lib.auto.Color;
import com.team1816.lib.auto.paths.AutoPath;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

import java.util.List;

public class MiddleToSideOne extends AutoPath {
    public MiddleToSideOne(Color color) {
        super(color);
    }
    @Override
    protected List<Pose2d> getWaypoints() {
        return List.of(
                new Pose2d(7.17, 4.07, Rotation2d.fromDegrees(155)),
                new Pose2d(5, 5.22, Rotation2d.fromDegrees(155))
        );
    }

    @Override
    protected List<Rotation2d> getWaypointHeadings() {
        return List.of(
                Rotation2d.fromDegrees(180),
                Rotation2d.fromDegrees(240)
        );
    }

    @Override
    protected boolean usingApp() {
        return true;
    }
}
