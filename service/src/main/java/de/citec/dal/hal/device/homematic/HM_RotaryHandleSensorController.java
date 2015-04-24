/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.homematic;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.homematic.HM_RotaryHandleSensorType;
import rst.homeautomation.device.homematic.HM_RotaryHandleSensorType.HM_RotaryHandleSensor;

/**
 *
 * @author thuxohl
 */
public class HM_RotaryHandleSensorController extends AbstractOpenHABDeviceController<HM_RotaryHandleSensor, HM_RotaryHandleSensor.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HM_RotaryHandleSensorType.HM_RotaryHandleSensor.getDefaultInstance()));
    }

    public HM_RotaryHandleSensorController(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, HM_RotaryHandleSensorType.HM_RotaryHandleSensor.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
