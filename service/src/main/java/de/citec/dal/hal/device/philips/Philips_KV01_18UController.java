/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.philips;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.philips.Philips_KV01_18UType.Philips_KV01_18U;

/**
 *
 * @author mpohling
 */
public class Philips_KV01_18UController extends AbstractOpenHABDeviceController<Philips_KV01_18U, Philips_KV01_18U.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Philips_KV01_18U.getDefaultInstance()));
    }

    public Philips_KV01_18UController(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, Philips_KV01_18U.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
