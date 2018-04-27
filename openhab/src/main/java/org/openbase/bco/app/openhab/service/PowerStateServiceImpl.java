package org.openbase.bco.app.openhab.service;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.PowerStateType.PowerState;

import java.util.concurrent.Future;

public class PowerStateServiceImpl<ST extends PowerStateOperationService & Unit<?>> extends OpenHABService<ST> implements PowerStateOperationService {

    public PowerStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setPowerState(PowerState powerState) throws CouldNotPerformException {
        switch (powerState.getValue()) {
            case ON:
                return executeCommand(OnOffType.ON);
            case OFF:
                return executeCommand(OnOffType.OFF);
            default:
                throw new CouldNotPerformException("Could not set power staet[" + powerState + "]");
        }
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        return unit.getPowerState();
    }
}
