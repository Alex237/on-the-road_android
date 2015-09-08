package com.mapzen.helpers;

import com.mapzen.valhalla.Instruction;
import com.mapzen.valhalla.Route;

import android.location.Location;

import java.util.ArrayList;

import static com.mapzen.helpers.DistanceFormatter.METERS_IN_ONE_MILE;

/**
 * In-route navigation state machine. Accepts raw location updates, attempts to snap them to the
 * route, and notifies listener of significant routing events.
 */
public class RouteEngine {
    public static final int APPROACH_RADIUS = 20;
    public static final int ALERT_RADIUS = 20;
    public static final int DESTINATION_RADIUS = 20;

    public enum RouteState {
        START,
        PRE_INSTRUCTION,
        INSTRUCTION,
        COMPLETE,
        LOST
    }

    public enum Milestone {
        ONE_MILE,
        QUARTER_MILE
    }

    private Route route;
    private RouteState routeState;
    private RouteListener listener;
    private Location location;
    private Location snapLocation;
    private Instruction currentInstruction;
    private ArrayList<Instruction> instructions;
    private Milestone lastMilestoneUpdate;

    public void onLocationChanged(final Location location) {
        if (routeState == RouteState.COMPLETE) {
            return;
        }

        this.location = location;
        snapLocation();

        if (routeState == RouteState.START) {
            listener.onAlertInstruction(0);
            routeState = RouteState.PRE_INSTRUCTION;
        }

        if (routeState == RouteState.COMPLETE) {
            listener.onUpdateDistance(0, 0);
        } else {
            listener.onUpdateDistance(route.getDistanceToNextInstruction(),
                    route.getRemainingDistanceToDestination());
        }

        if (routeState == RouteState.LOST) {
            return;
        }

        checkApproachMilestone(Milestone.ONE_MILE, METERS_IN_ONE_MILE);
        checkApproachMilestone(Milestone.QUARTER_MILE, METERS_IN_ONE_MILE / 4);

        if (routeState == RouteState.PRE_INSTRUCTION
                && route.getDistanceToNextInstruction() < ALERT_RADIUS
                && route.getNextInstructionIndex() != null) {
            final int nextIndex = route.getNextInstructionIndex();
            listener.onAlertInstruction(nextIndex);
            routeState = RouteState.INSTRUCTION;
        }

        final Instruction nextInstruction = route.getNextInstruction();

        if (instructions != null && !currentInstruction.equals(nextInstruction)) {
            routeState = RouteState.PRE_INSTRUCTION;
            int nextIndex = instructions.indexOf(currentInstruction);
            listener.onInstructionComplete(nextIndex);
        }

        currentInstruction = route.getNextInstruction();
    }

    private void checkApproachMilestone(Milestone milestone, double distance) {
        if (routeState == RouteState.PRE_INSTRUCTION
                && Math.abs(route.getDistanceToNextInstruction() - distance) < APPROACH_RADIUS
                && lastMilestoneUpdate != milestone
                && route.getNextInstructionIndex() != null) {
            final int nextIndex = route.getNextInstructionIndex();
            listener.onApproachInstruction(nextIndex, milestone);
            lastMilestoneUpdate = milestone;
        }
    }

    private void snapLocation() {
        snapLocation = route.snapToRoute(location);

        if (snapLocation != null) {
            listener.onSnapLocation(location, snapLocation);
        }

        if (youHaveArrived()) {
            routeState = RouteState.COMPLETE;
            listener.onRouteComplete();
        }

        if (routeState != RouteState.START) {
            if (route.isLost()) {
                routeState = RouteState.LOST;
                listener.onRecalculate(location);
            }
        }
    }

    private boolean youHaveArrived() {
        return getLocationForDestination() != null
                && snapLocation != null
                && snapLocation.distanceTo(getLocationForDestination()) < DESTINATION_RADIUS;
    }

    private Location getLocationForDestination() {
        if (route.getRouteInstructions() == null) {
            return null;
        }

        final int destinationIndex = route.getRouteInstructions().size() - 1;
        return route.getRouteInstructions().get(destinationIndex).getLocation();
    }

    public void setRoute(Route route) {
        this.route = route;
        routeState = RouteState.START;
        instructions = route.getRouteInstructions();
        if (instructions != null) {
            currentInstruction = instructions.get(0);
        }
    }

    public void setListener(RouteListener listener) {
        this.listener = listener;
    }
}
