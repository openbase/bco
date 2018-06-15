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

import org.openbase.bco.app.openhab.manager.transform.StopMoveStateTransformer;
import org.openbase.bco.app.openhab.manager.transform.UpDownStateTransformer;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.BlindStateType.BlindState;

import java.util.concurrent.Future;

public class BlindStateServiceImpl<ST extends BlindStateOperationService & Unit<?>> extends OpenHABService<ST> implements BlindStateOperationService {

    BlindStateServiceImpl(ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setBlindState(BlindState blindState) throws CouldNotPerformException {
        switch (blindState.getValue()) {
            case UP:
                return executeCommand(UpDownStateTransformer.transform(blindState));
            case DOWN:
                return executeCommand(UpDownStateTransformer.transform(blindState));
            case STOP:
                return executeCommand(StopMoveStateTransformer.transform(blindState));
            default:
                throw new CouldNotPerformException("Cannot set blind state to unknown state [" + blindState + "]");
        }
    }

    @Override
    public BlindState getBlindState() throws NotAvailableException {
        return unit.getBlindState();
    }
}
