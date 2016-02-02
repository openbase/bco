/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

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

	public BrightnessSensorController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, BrightnessSensor.Builder builder) throws InstantiationException, CouldNotPerformException {
		super(config, BrightnessSensorController.class, unitHost, builder);
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
