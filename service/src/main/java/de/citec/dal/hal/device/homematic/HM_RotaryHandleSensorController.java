/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.homematic;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.BatteryController;
import de.citec.dal.hal.unit.HandleSensorController;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.VerificationFailedException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.devices.homematic.HM_RotaryHandleSensorType;
import rst.homeautomation.devices.homematic.HM_RotaryHandleSensorType.HM_RotaryHandleSensor;

/**
 *
 * @author thuxohl
 */
public class HM_RotaryHandleSensorController extends AbstractOpenHABDeviceController<HM_RotaryHandleSensor, HM_RotaryHandleSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HM_RotaryHandleSensorType.HM_RotaryHandleSensor.getDefaultInstance()));
    }

    public HM_RotaryHandleSensorController(final String id, final String label, final Location location) throws VerificationFailedException, InstantiationException {
        super(id, label, location, HM_RotaryHandleSensor.newBuilder());
        this.registerUnit(new HandleSensorController(label, this, data.getHandleSensorBuilder()));
        this.registerUnit(new BatteryController(label, this, data.getBatteryBuilder()));
    }
}
