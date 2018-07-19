package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.unit.dal.MonitorDataType.MonitorData;
import rst.domotic.unit.UnitConfigType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class MonitorController extends AbstractDALUnitController<MonitorData, MonitorData.Builder> implements Monitor {
    
    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MonitorData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(StandbyState.getDefaultInstance()));
    }
    
    private PowerStateOperationService powerStateService;
    private StandbyStateOperationService standbyStateService;
    
    public MonitorController(final UnitHost unitHost, final MonitorData.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(MonitorController.class, unitHost, builder);
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to PowerState [" + state + "]");
        try {
            Services.verifyOperationServiceState(state);
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        return powerStateService.setPowerState(state);
    }
    
    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("powerState", ex);
        }
    }
    
    @Override
    public Future<ActionFuture> setStandbyState(final StandbyState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to StandbyState [" + state + "]");
        try {
            Services.verifyOperationServiceState(state);
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        return standbyStateService.setStandbyState(state);
    }
    
    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        try {
            return getData().getStandbyState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("standbyState", ex);
        }
    }
}
