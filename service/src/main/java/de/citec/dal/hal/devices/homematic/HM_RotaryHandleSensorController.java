/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.homematic;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.OpenClosedTiltedStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractHardwareController;
import de.citec.dal.hal.al.BatteryStateController;
import de.citec.dal.hal.al.HandleSensorController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.homematic.HM_RotaryHandleSensorType;
import rst.devices.homematic.HM_RotaryHandleSensorType.HM_RotaryHandleSensor;

/**
 *
 * @author thuxohl
 */
public class HM_RotaryHandleSensorController extends AbstractHardwareController<HM_RotaryHandleSensor, HM_RotaryHandleSensor.Builder> {

    private final static String COMPONENT_HANDLE_SENSOR = "HandleSensor";
    private final static String COMPONENT_BATTERY_STATE = "BatteryState";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HM_RotaryHandleSensorType.HM_RotaryHandleSensor.getDefaultInstance()));
    }

    private final HandleSensorController handleSensor;
    private final BatteryStateController batteryState;

    public HM_RotaryHandleSensorController(final String id, final Location location) throws RSBBindingException {
        super(id, location, HM_RotaryHandleSensor.newBuilder());

        builder.setId(id);
        this.handleSensor = new HandleSensorController(COMPONENT_HANDLE_SENSOR, this, builder.getHandleSensorBuilder());
        this.batteryState = new BatteryStateController(COMPONENT_BATTERY_STATE, this, builder.getBatterystateBuilder());
        this.register(handleSensor);
        this.register(batteryState);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_HANDLE_SENSOR, getClass().getMethod("updateHandleSensor", org.openhab.core.library.types.StringType.class));
        halFunctionMapping.put(COMPONENT_BATTERY_STATE, getClass().getMethod("updateBatteryState", org.openhab.core.library.types.DecimalType.class));
    }

    public void updateHandleSensor(org.openhab.core.library.types.StringType type) {
        try {
            handleSensor.updateOpenClosedTiltedState(OpenClosedTiltedStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from StringType to OpenClosedTiltedState!", ex);
        }
    }

    public void updateBatteryState(org.openhab.core.library.types.DecimalType value) {
        batteryState.updateBatteryLevel(value.floatValue());
    }
}
