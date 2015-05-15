/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import de.citec.jul.exception.InstantiationException;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.fibaro.Fibaro_FGMS_001Type.Fibaro_FGMS_001;

/**
 *
 * @author mpohling
 */
public class Fibaro_FGMS_001Controller extends AbstractOpenHABDeviceController<Fibaro_FGMS_001, Fibaro_FGMS_001.Builder> {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Fibaro_FGMS_001.getDefaultInstance()));
	}

	public Fibaro_FGMS_001Controller(final DeviceConfigType.DeviceConfig config) throws InstantiationException, CouldNotTransformException {
		super(config, Fibaro_FGMS_001.newBuilder());
		try {
			registerUnits(config.getUnitConfigList());
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}
}
