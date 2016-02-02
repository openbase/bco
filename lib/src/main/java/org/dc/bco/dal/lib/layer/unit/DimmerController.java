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


import org.dc.bco.dal.lib.layer.service.DimService;
import org.dc.bco.dal.lib.layer.service.PowerService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.DimmerType.Dimmer;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class DimmerController extends AbstractUnitController<Dimmer, Dimmer.Builder> implements DimmerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Dimmer.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private final PowerService powerService;
    private final DimService dimmService;

    public DimmerController(final UnitConfigType.UnitConfig config, final UnitHost unitHost, Dimmer.Builder builder) throws org.dc.jul.exception.InstantiationException, CouldNotPerformException {
        super(config, DimmerController.class, unitHost, builder);
        this.powerService = getServiceFactory().newPowerService(this);
        this.dimmService = getServiceFactory().newDimmService(this);
    }

    public void updatePower(final PowerState.State value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Dimmer.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setPower(final PowerState.State state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to Power [" + state.name() + "]");
        powerService.setPower(state);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power", ex);
        }
    }

    public void updateDim(final Double value) throws CouldNotPerformException {
        logger.debug("Apply dim Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Dimmer.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setValue(value);
            if(value == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply dim Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public void setDim(Double dimm) throws CouldNotPerformException {
        dimmService.setDim(dimm);
    }

    @Override
    public Double getDim() throws NotAvailableException {
        try {
            return getData().getValue();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("dim", ex);
        }
    }
}
