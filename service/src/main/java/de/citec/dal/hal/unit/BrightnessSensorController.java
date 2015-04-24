/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.BrightnessSensorType;
import rst.homeautomation.unit.BrightnessSensorType.BrightnessSensor;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class BrightnessSensorController extends AbstractUnitController<BrightnessSensor, BrightnessSensor.Builder> implements BrightnessSensorInterface {

	static {
		DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessSensorType.BrightnessSensor.getDefaultInstance()));
	}

	public BrightnessSensorController(final UnitConfigType.UnitConfig config, Device device, BrightnessSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
		super(config, BrightnessSensorController.class, device, builder);
	}

	public void updateBrightness(final float brightness) {
		data.setBrightness(brightness);
		notifyChange();
	}

	@Override
	public Double getBrightness() throws CouldNotPerformException {
		return (double) data.getBrightness();
	}
}
