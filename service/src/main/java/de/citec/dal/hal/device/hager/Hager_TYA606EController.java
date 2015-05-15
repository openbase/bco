/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.hager;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.hager.Hager_TYA606EType.Hager_TYA606E;

/**
 *
 * @author mpohling
 */
public class Hager_TYA606EController extends AbstractOpenHABDeviceController<Hager_TYA606E, Hager_TYA606E.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Hager_TYA606E.getDefaultInstance()));
    }

    public Hager_TYA606EController(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, Hager_TYA606E.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
