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
import rst.homeautomation.device.philips.PH_Hue_GU10Type;
import rst.homeautomation.device.philips.PH_Hue_GU10Type.PH_Hue_GU10;

/**
 *
 * @author mpohling
 */
public class PH_Hue_GU10Controller extends AbstractOpenHABDeviceController<PH_Hue_GU10, PH_Hue_GU10.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PH_Hue_GU10Type.PH_Hue_GU10.getDefaultInstance()));
    }

    public PH_Hue_GU10Controller(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, PH_Hue_GU10Type.PH_Hue_GU10.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }
}
