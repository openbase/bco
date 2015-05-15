/*

 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.gira;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.gira.Gira_5133Type.Gira_5133;

/**
 *
 * @author mpohling
 */
public class Gira_5133Controller extends AbstractOpenHABDeviceController<Gira_5133, Gira_5133.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Gira_5133.getDefaultInstance()));
    }

    public Gira_5133Controller(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, Gira_5133.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
