/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.devices.homematic;

import de.citec.dal.data.Location;
import de.citec.dal.data.transform.OpenClosedTiltedStateTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.al.BatteryController;
import de.citec.dal.hal.al.HandleSensorController;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.homematic.HM_RotaryHandleSensorType;
import rst.devices.homematic.HM_RotaryHandleSensorType.HM_RotaryHandleSensor;
import rst.homeautomation.openhab.StringType;

/**
 *
 * @author thuxohl
 */
public class HM_RotaryHandleSensorController extends AbstractDeviceController<HM_RotaryHandleSensor, HM_RotaryHandleSensor.Builder> {

    private final static String COMPONENT_HANDLE_SENSOR = "HandleSensor";
    private final static String COMPONENT_BATTERY = "Battery";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HM_RotaryHandleSensorType.HM_RotaryHandleSensor.getDefaultInstance()));
    }

    private final HandleSensorController handleSensor;
    private final BatteryController battery;

    public HM_RotaryHandleSensorController(final String id, final String label, final Location location) throws RSBBindingException {
        super(id, label, location, HM_RotaryHandleSensor.newBuilder());

        builder.setId(id);
        this.handleSensor = new HandleSensorController(COMPONENT_HANDLE_SENSOR, label, this, builder.getHandleSensorBuilder());
        this.battery = new BatteryController(COMPONENT_BATTERY, label, this, builder.getBatteryBuilder());
        this.register(handleSensor);
        this.register(battery);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_HANDLE_SENSOR, getClass().getMethod("updateHandleSensor", StringType.String.class));
        halFunctionMapping.put(COMPONENT_BATTERY, getClass().getMethod("updateBatteryLevel", double.class));
    }

    public void updateHandleSensor(StringType.String type) {
        try {
            handleSensor.updateOpenClosedTiltedState(OpenClosedTiltedStateTransformer.transform(type));
        } catch (RSBBindingException ex) {
            logger.error("Not able to transform from StringType to OpenClosedTiltedState!", ex);
        }
    }

    public void updateBatteryLevel(double value) {
        battery.updateBatteryLevel((float) value);
    }
}
