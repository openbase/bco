/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.philips;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.AmbientLightController;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.philips.PH_Hue_E27Type;
import rst.homeautomation.device.philips.PH_Hue_E27Type.PH_Hue_E27;

/**
 *
 * @author mpohling
 */
public class PH_Hue_E27Controller extends AbstractOpenHABDeviceController<PH_Hue_E27, PH_Hue_E27.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PH_Hue_E27Type.PH_Hue_E27.getDefaultInstance()));
    }

    public PH_Hue_E27Controller(final String label, final Location location) throws InstantiationException {
		super(label, location, PH_Hue_E27.newBuilder());
        this.registerUnit(new AmbientLightController(label, this, data.getAmbientLightBuilder(), getDefaultServiceFactory()));
	}
	
	@Deprecated
    public PH_Hue_E27Controller(final String id, final String label, final Location location) throws InstantiationException {
        super(id, label, location, PH_Hue_E27.newBuilder());
        this.registerUnit(new AmbientLightController(label, this, data.getAmbientLightBuilder(), getDefaultServiceFactory()));
    }
}
