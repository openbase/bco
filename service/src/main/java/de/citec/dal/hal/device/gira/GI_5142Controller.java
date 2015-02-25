/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.gira;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.ButtonController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.gira.GI_5142Type;

/**
 *
 * @author mpohling
 */
public class GI_5142Controller extends AbstractOpenHABDeviceController<GI_5142Type.GI_5142, GI_5142Type.GI_5142.Builder> {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(GI_5142Type.GI_5142.getDefaultInstance()));
	}

	public GI_5142Controller(final String label, final String[] unitLabel, final Location location) throws InstantiationException {
		super(label, location, GI_5142Type.GI_5142.newBuilder());
		try {
			this.registerUnit(new ButtonController(unitLabel[0], this, data.getButton0Builder()));
			this.registerUnit(new ButtonController(unitLabel[1], this, data.getButton1Builder()));
			this.registerUnit(new ButtonController(unitLabel[2], this, data.getButton2Builder()));
			this.registerUnit(new ButtonController(unitLabel[3], this, data.getButton3Builder()));
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}
}
