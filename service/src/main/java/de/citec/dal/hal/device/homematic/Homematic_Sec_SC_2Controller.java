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
import rst.homeautomation.device.homematic.Homematic_HM_Sec_SC_2Type.Homematic_HM_Sec_SC_2;

/**
 *
 * @author mpohling
 */
public class Homematic_Sec_SC_2Controller extends AbstractOpenHABDeviceController<Homematic_HM_Sec_SC_2, Homematic_HM_Sec_SC_2.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Homematic_HM_Sec_SC_2.getDefaultInstance()));
    }

    public Homematic_Sec_SC_2Controller(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, Homematic_HM_Sec_SC_2.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
