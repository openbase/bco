package org.openbase.bco.manager.device.binding.openhab.service;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.bco.dal.lib.layer.service.operation.IntensityStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.IntensityStateType.IntensityState;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * @param <UNIT>
 */
@Deprecated
public class IntensityStateServiceImpl<UNIT extends IntensityStateOperationService & Unit<?>> extends OpenHABService<UNIT> implements IntensityStateOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntensityStateServiceImpl.class);

    public IntensityStateServiceImpl(final UNIT unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setIntensityState(final IntensityState intensityState) throws CouldNotPerformException {
        return executeCommand(OpenHABCommandFactory.newPercentCommand(intensityState.getIntensity()));
    }

    @Override
    public IntensityState getIntensityState() throws NotAvailableException {
        return unit.getIntensityState();
    }
}
