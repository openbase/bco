package org.openbase.bco.app.cloud.connector.google;

import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

public enum UnitTypeDeviceTypeMapping {

    LIGHT_DEVICE_TYPE_MAPPING(UnitType.LIGHT, DeviceType.LIGHT),
    DIMMABLE_LIGHT_DEVICE_TYPE_MAPPING(UnitType.DIMMABLE_LIGHT, DeviceType.LIGHT),
    COLORABLE_LIGHT_DEVICE_TYPE_MAPPING(UnitType.COLORABLE_LIGHT, DeviceType.LIGHT);

    public static final String POSTFIX = "_DEVICE_TYPE_MAPPING";

    private final UnitType unitType;
    private final DeviceType deviceType;

    UnitTypeDeviceTypeMapping(final UnitType unitType, final DeviceType deviceType) {
        this.unitType = unitType;
        this.deviceType = deviceType;
    }

    public UnitType getUnitType() {
        return unitType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public static UnitTypeDeviceTypeMapping getByUnitType(final UnitType unitType) throws NotAvailableException {
        try {
            return UnitTypeDeviceTypeMapping.valueOf(unitType.name() + POSTFIX);
        } catch (IllegalArgumentException ex) {
            throw new NotAvailableException("UnitTypeDeviceTypeMapping for unitType[" + unitType.name() + "]");
        }
    }
}
