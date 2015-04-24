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
import rst.homeautomation.device.hager.HA_TYA663AType;

/**
 *
 * @author mpohling
 */
public class HA_TYA663AController extends AbstractOpenHABDeviceController<HA_TYA663AType.HA_TYA663A, HA_TYA663AType.HA_TYA663A.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HA_TYA663AType.HA_TYA663A.getDefaultInstance()));
    }

    public HA_TYA663AController(final DeviceConfigType.DeviceConfig config) throws CouldNotTransformException, InstantiationException {
        super(config, HA_TYA663AType.HA_TYA663A.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
