/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
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

	public BrightnessSensorController(final UnitHost unitHost, BrightnessSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
		super(BrightnessSensorController.class, unitHost, builder);
	}

	public void updateBrightness(final Double value) throws CouldNotPerformException {
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
            return getData().getBrightness();
        } catch(CouldNotPerformException ex) {
            throw new NotAvailableException("brightness", ex);
        }
	}
}
