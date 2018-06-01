package org.openbase.bco.app.openhab.manager.service;

import org.openbase.bco.app.openhab.manager.transform.StandbyStateTransformer;
import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.StandbyStateType.StandbyState;

import java.util.concurrent.Future;

public class StandbyStateServiceImpl<ST extends StandbyStateOperationService & Unit<?>> extends OpenHABService<ST> implements StandbyStateOperationService {

    StandbyStateServiceImpl(ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setStandbyState(StandbyState standbyState) throws CouldNotPerformException {
        return executeCommand(StandbyStateTransformer.transform(standbyState));
    }

    @Override
    public StandbyState getStandbyState() throws NotAvailableException {
        return unit.getStandbyState();
    }
}
