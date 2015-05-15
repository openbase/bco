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
import rst.homeautomation.device.hager.Hager_TYA663AType.Hager_TYA663A;

/**
 *
 * @author mpohling
 */
public class Hager_TYA663AController extends AbstractOpenHABDeviceController<Hager_TYA663A, Hager_TYA663A.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Hager_TYA663A.getDefaultInstance()));
    }

    public Hager_TYA663AController(final DeviceConfigType.DeviceConfig config) throws CouldNotTransformException, InstantiationException {
        super(config, Hager_TYA663A.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
