package org.openbase.bco.app.openhab.manager.service;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.bco.app.openhab.manager.transform.ServiceStateCommandTransformerPool;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.StandbyStateType.StandbyState;

import java.util.concurrent.Future;

public class StandbyStateServiceImpl<ST extends StandbyStateOperationService & Unit<?>> extends OpenHABService<ST> implements StandbyStateOperationService {

    public StandbyStateServiceImpl(ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
        return executeCommand(ServiceStateCommandTransformerPool.getInstance().getTransformer(StandbyState.class, OnOffType.class).transform(standbyState));
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        return unit.getStandbyState();
    }
}
