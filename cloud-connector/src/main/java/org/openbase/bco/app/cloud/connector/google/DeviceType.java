package org.openbase.bco.app.cloud.connector.google;

public enum DeviceType {

    LIGHT;

    public static final String REPRESENTATION_PREFIX = "action.devices.types.";

    private final String representation;

    DeviceType() {
        this.representation = REPRESENTATION_PREFIX + this.name();
    }

    DeviceType(final String postfix) {
        this.representation = REPRESENTATION_PREFIX + postfix;
    }

    public String getRepresentation() {
        return representation;
    }
}
