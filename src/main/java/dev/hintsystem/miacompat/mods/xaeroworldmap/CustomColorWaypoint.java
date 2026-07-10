package dev.hintsystem.miacompat.mods.xaeroworldmap;

import xaero.map.mods.gui.Waypoint;

public class CustomColorWaypoint extends Waypoint {
    protected int customColor;

    public CustomColorWaypoint(Object original, int customColor, boolean editable, String setName, double dimDiv) {
        super(original, editable, setName, dimDiv);
        this.customColor = customColor;
    }

    @Override
    public int getColor() { return customColor; }
}
