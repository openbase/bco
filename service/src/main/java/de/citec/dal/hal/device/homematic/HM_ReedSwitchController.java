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
import rst.homeautomation.device.homematic.HM_ReedSwitchType;
import rst.homeautomation.device.homematic.HM_ReedSwitchType.HM_ReedSwitch;

/**
 *
 * @author mpohling
 */
public class HM_ReedSwitchController extends AbstractOpenHABDeviceController<HM_ReedSwitch, HM_ReedSwitch.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HM_ReedSwitchType.HM_ReedSwitch.getDefaultInstance()));
    }

    public HM_ReedSwitchController(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, HM_ReedSwitchType.HM_ReedSwitch.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
