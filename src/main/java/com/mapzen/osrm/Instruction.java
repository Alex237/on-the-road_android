package com.mapzen.osrm;

import com.mapzen.helpers.DistanceFormatter;
import com.mapzen.ontheroad.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;

import java.util.Locale;

import static com.mapzen.osrm.Route.SNAP_PROVIDER;

public class Instruction {
    public static final int NO_TURN = 0;
    public static final int GO_STRAIGHT = 1;
    public static final int TURN_SLIGHT_RIGHT = 2;
    public static final int TURN_RIGHT = 3;
    public static final int TURN_SHARP_RIGHT = 4;
    public static final int U_TURN = 5;
    public static final int TURN_SHARP_LEFT = 6;
    public static final int TURN_LEFT = 7;
    public static final int TURN_SLIGHT_LEFT = 8;
    public static final int REACH_VIA_POINT = 9;
    public static final int HEAD_ON = 10;
    public static final int ENTER_ROUND_ABOUT = 11;
    public static final int LEAVE_ROUND_ABOUT = 12;
    public static final int STAY_ON_ROUND_ABOUT = 13;
    public static final int START_AT_END_OF_STREET = 14;
    public static final int YOU_HAVE_ARRIVED = 15;
    public static final int ENTER_AGAINST_ALLOWED_DIRECTION = 16;
    public static final int LEAVE_AGAINST_ALLOWED_DIRECTION = 17;
    public static final int INSTRUCTION_COUNT = 18;

    private JSONArray json;
    private int turn, distanceInMeters;
    private Location location = new Location(SNAP_PROVIDER);
    private int liveDistanceToNext = -1;

    public int getLiveDistanceToNext() {
        return liveDistanceToNext;
    }

    public void setLiveDistanceToNext(int liveDistanceToNext) {
        this.liveDistanceToNext = liveDistanceToNext;
    }

    public Instruction(JSONArray json) {
        if (json.length() < 8) {
            throw new JSONException("too few arguments");
        }
        this.json = json;
        setTurnInstruction(parseTurnInstruction(json));
        setDistance(json.getInt(2));
    }

    /**
     * Used for testing. Do not remove.
     */
    @SuppressWarnings("unused")
    protected Instruction() {
    }

    public int getTurnInstruction() {
        return turn;
    }

    public void setTurnInstruction(int turn) {
        this.turn = turn;
    }

    public String getHumanTurnInstruction(Context context) {
        switch (turn) {
            case NO_TURN:
                return context.getString(R.string.no_turn);
            case GO_STRAIGHT:
                return context.getString(R.string.go_straight);
            case TURN_SLIGHT_RIGHT:
                return context.getString(R.string.turn_slight_right);
            case TURN_RIGHT:
                return context.getString(R.string.turn_right);
            case TURN_SHARP_RIGHT:
                return context.getString(R.string.turn_sharp_right);
            case U_TURN:
                return context.getString(R.string.u_turn);
            case TURN_SHARP_LEFT:
                return context.getString(R.string.turn_sharp_left);
            case TURN_LEFT:
                return context.getString(R.string.turn_left);
            case TURN_SLIGHT_LEFT:
                return context.getString(R.string.turn_slight_left);
            case REACH_VIA_POINT:
                return context.getString(R.string.reach_via_point);
            case HEAD_ON:
                return context.getString(R.string.head_on);
            case ENTER_ROUND_ABOUT:
                return context.getString(R.string.enter_round_about);
            case LEAVE_ROUND_ABOUT:
                return context.getString(R.string.leave_round_about);
            case STAY_ON_ROUND_ABOUT:
                return context.getString(R.string.stay_on_round_about);
            case START_AT_END_OF_STREET:
                return context.getString(R.string.start_at_end_of_street);
            case YOU_HAVE_ARRIVED:
                return context.getString(R.string.you_have_arrived);
            case ENTER_AGAINST_ALLOWED_DIRECTION:
                return context.getString(R.string.enter_against_allowed_direction);
            case LEAVE_AGAINST_ALLOWED_DIRECTION:
                return context.getString(R.string.leave_against_allowed_direction);
            default:
                return context.getString(R.string.no_turn);
        }
    }

    public boolean skip() {
        String raw = json.getString(1);
        return raw.startsWith("{") && raw.endsWith("}");
    }

    public String getName() {
        String raw = json.getString(1);
        if (raw.startsWith("{") && raw.endsWith("}")) {
            JSONObject nameObject = new JSONObject(raw);
            return nameObject.getString("highway");
        } else {
            return raw;
        }
    }

    public int getDistance() {
        return distanceInMeters;
    }

    public void setDistance(int distanceInMeters) {
        this.distanceInMeters = distanceInMeters;
    }

    public String getFormattedDistance() {
        return DistanceFormatter.format(distanceInMeters);
    }

    public String getDirection() {
        return json.getString(6);
    }

    public int getPolygonIndex() {
        return json.getInt(3);
    }

    public float getDirectionAngle() {
        String direction = getDirection();
        float angle = 0;
        if (direction.equals("NE")) {
            angle = 315.0f;
        } else if (direction.equals("E")) {
            angle = 270.0f;
        } else if (direction.equals("SE")) {
            angle = 225.0f;
        } else if (direction.equals("S")) {
            angle = 180.0f;
        } else if (direction.equals("SW")) {
            angle = 135.0f;
        } else if (direction.equals("W")) {
            angle = 90.0f;
        } else if (direction.equals("NW")) {
            angle = 45.0f;
        }
        return angle;
    }

    public int getRotationBearing() {
        return 360 - json.getInt(7);
    }

    public int getBearing() {
        return json.getInt(7);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    private String getFullInstructionBeforePattern() {
        String controllingGluePhrase = "and continue on for";
        String pattern = "%s %s " + controllingGluePhrase + " %s";
        if (turn == HEAD_ON || turn == GO_STRAIGHT) {
            controllingGluePhrase = "for";
            pattern = "%s %s " + controllingGluePhrase + " %s";
        } else if (turn ==YOU_HAVE_ARRIVED) {
            pattern = "%s %s";
        }

        return pattern;
    }

    public String getFullInstruction(Context context) {
        return getFullInstructionBeforeAction(context);
    }

    public String getFullInstructionBeforeAction(Context context) {
        return String.format(Locale.US,
                getFullInstructionBeforePattern(),
                getHumanTurnInstruction(context),
                getName(),
                DistanceFormatter.format(distanceInMeters, true));
    }

    public String getFullInstructionAfterAction(Context context) {
        if (turn == YOU_HAVE_ARRIVED) {
            return getFullInstructionBeforeAction(context);
        }

        String pattern = "Continue on %s for %s";
        return String.format(Locale.US, pattern, getName(), DistanceFormatter.format(getDistance(),
                false));
    }

    public String getSimpleInstruction(Context context) {
        return String.format(Locale.US, "%s %s", getHumanTurnInstruction(context), getName());
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Instruction: (%.5f, %.5f) %s %s LiveDistanceTo: %d",
                location.getLatitude(), location.getLongitude(), turn,
                getName(), liveDistanceToNext);
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        Instruction other = (Instruction) obj;
        return (getTurnInstruction() == other.getTurnInstruction()
                && getBearing() == other.getBearing()
                && getLocation().getLatitude() == other.getLocation().getLatitude()
                && getLocation().getLongitude() == other.getLocation().getLongitude());
    }

    private int parseTurnInstruction(JSONArray json) {
        String turn = json.getString(0);
        String[] split = turn.split("-");
        return Integer.valueOf(split[0]);
    }

    public String getSimpleInstructionAfterAction(Context context) {
        if (turn == YOU_HAVE_ARRIVED) {
            return getFullInstructionBeforeAction(context);
        }
        String pattern = "Continue on %s";
        return String.format(Locale.US, pattern, getName(), false);
    }
}
