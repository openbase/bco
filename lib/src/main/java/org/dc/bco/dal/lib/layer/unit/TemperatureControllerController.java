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


import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.unit.TemperatureControllerType.TemperatureController;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class TemperatureControllerController extends AbstractUnitController<TemperatureController, TemperatureController.Builder> implements TemperatureControllerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureController.getDefaultInstance()));
    }

    private final TargetTemperatureService targetTemperatureService;


    public TemperatureControllerController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, final TemperatureController.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(config, TemperatureControllerController.class, unitHost, builder);
        this.targetTemperatureService = getServiceFactory().newTargetTemperatureService(this);
    }

    @Override
    public void setTargetTemperature(final Double value) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to target temperature [" + value + "]");
        targetTemperatureService.setTargetTemperature(value);
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        try {
            return getData().getTargetTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("target temperature", ex);
        }
    }

    public void updateTargetTemperature(final Double value) throws CouldNotPerformException {
        logger.debug("Apply target temperature Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureController.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTargetTemperature(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply target temperature Update[" + value + "] for " + this + "!", ex);
        }
    }
    
    public void updateTemperature(final Double value) throws CouldNotPerformException {
        logger.debug("Apply actual temperature Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureController.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setActualTemperature(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply actual temperature Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Double getTemperature() throws CouldNotPerformException {
        try {
            return getData().getActualTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("actual temperature", ex);
        }
    }
}
