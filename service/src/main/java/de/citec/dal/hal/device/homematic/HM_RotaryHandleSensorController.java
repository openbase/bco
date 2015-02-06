/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.homematic;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.data.transform.OpenClosedTiltedStateTransformer;
import de.citec.dal.exception.DALException;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.AbstractDeviceController;
import de.citec.dal.hal.unit.BatteryController;
import de.citec.dal.hal.unit.HandleSensorController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.devices.homematic.HM_RotaryHandleSensorType;
import rst.devices.homematic.HM_RotaryHandleSensorType.HM_RotaryHandleSensor;

/**
 *
 * @author thuxohl
 */
public class HM_RotaryHandleSensorController extends AbstractOpenHABDeviceController<HM_RotaryHandleSensor, HM_RotaryHandleSensor.Builder> {

    private final static String COMPONENT_HANDLE_SENSOR = "HandleSensor";
    private final static String COMPONENT_BATTERY = "Battery";

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(HM_RotaryHandleSensorType.HM_RotaryHandleSensor.getDefaultInstance()));
    }

    private final HandleSensorController handleSensor;
    private final BatteryController battery;

    public HM_RotaryHandleSensorController(final String id, final String label, final Location location) throws VerificationFailedException, DALException, InstantiationException {
        super(id, label, location, HM_RotaryHandleSensor.newBuilder());

        data.setId(id);
        this.handleSensor = new HandleSensorController(COMPONENT_HANDLE_SENSOR, label, this, data.getHandleSensorBuilder());
        this.battery = new BatteryController(COMPONENT_BATTERY, label, this, data.getBatteryBuilder());
        this.register(handleSensor);
        this.register(battery);
    }

    @Override
    protected void initHardwareMapping() throws NoSuchMethodException, SecurityException {
        halFunctionMapping.put(COMPONENT_HANDLE_SENSOR, getClass().getMethod("updateHandleSensor", String.class));
        halFunctionMapping.put(COMPONENT_BATTERY, getClass().getMethod("updateBatteryLevel", double.class));
    }

    public void updateHandleSensor(String type) {
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
