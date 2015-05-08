/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.extension.protobuf.ClosableDataBuilder;
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

	public void updateBrightness(final float value) throws CouldNotPerformException {
        logger.debug("Apply brightness Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<BrightnessSensor.Builder> dataBuilder = getDataBuilder(this)) {
             dataBuilder.getInternalBuilder().setBrightness(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightness Update[" + value + "] for " + this + "!", ex);
        }
	}

	@Override
	public Double getBrightness() throws NotAvailableException {
        try {
            //TODO mpohling: check double or float?
            return (double) getData().getBrightness();
        } catch(CouldNotPerformException ex) {
            throw new NotAvailableException("brightness", ex);
        }
	}
}
