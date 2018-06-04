package org.openbase.bco.app.openhab.manager.service;

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
