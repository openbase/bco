package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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


import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
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

    private TargetTemperatureOperationService targetTemperatureService;

    public TemperatureControllerController(final UnitHost unitHost, final TemperatureController.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(TemperatureControllerController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.targetTemperatureService = getServiceFactory().newTargetTemperatureService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }


    @Override
    public Future<Void> setTargetTemperature(final Double value) throws CouldNotPerformException {
        logger.debug("Set " + getType().name() + "[" + getLabel() + "] to target temperature [" + value + "]");
        return targetTemperatureService.setTargetTemperature(value);
    }

    @Override
    public Double getTargetTemperature() throws NotAvailableException {
        try {
            return getData().getTargetTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("target temperature", ex);
        }
    }

    public void updateTargetTemperatureProvider(final Double value) throws CouldNotPerformException {
        logger.info("Apply target temperature Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureController.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setTargetTemperature(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply target temperature Update[" + value + "] for " + this + "!", ex);
        }
        
        logger.info("Target temperature update applied");
    }

    public void updateTemperatureProvider(final Double value) throws CouldNotPerformException {
        logger.debug("Apply actual temperature Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<TemperatureController.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setActualTemperature(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply actual temperature Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Double getTemperature() throws NotAvailableException {
        try {
            return getData().getActualTemperature();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("actual temperature", ex);
        }
    }
}
