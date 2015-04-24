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
import rst.homeautomation.device.plugwise.PW_PowerPlugType;
import rst.homeautomation.device.plugwise.PW_PowerPlugType.PW_PowerPlug;

/**
 *
 * @author mpohling
 */
public class PW_PowerPlugController extends AbstractOpenHABDeviceController<PW_PowerPlug, PW_PowerPlug.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PW_PowerPlugType.PW_PowerPlug.getDefaultInstance()));
    }

    public PW_PowerPlugController(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, PW_PowerPlugType.PW_PowerPlug.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
