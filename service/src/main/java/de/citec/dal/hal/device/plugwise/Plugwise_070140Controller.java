/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.plugwise;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.plugwise.Plugwise_070140Type.Plugwise_070140;

/**
 *
 * @author mpohling
 */
public class Plugwise_070140Controller extends AbstractOpenHABDeviceController<Plugwise_070140, Plugwise_070140.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Plugwise_070140.getDefaultInstance()));
    }

    public Plugwise_070140Controller(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, Plugwise_070140.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
