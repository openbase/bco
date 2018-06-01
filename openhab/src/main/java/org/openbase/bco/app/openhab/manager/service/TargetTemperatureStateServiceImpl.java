package org.openbase.bco.app.openhab.manager.service;

import org.openbase.bco.app.openhab.manager.transform.TemperatureStateTransformer;
import org.openbase.bco.dal.lib.layer.service.operation.TargetTemperatureStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.TemperatureStateType.TemperatureState;

import java.util.concurrent.Future;

public class TargetTemperatureStateServiceImpl<ST extends TargetTemperatureStateOperationService & Unit<?>> extends OpenHABService<ST> implements TargetTemperatureStateOperationService {

    TargetTemperatureStateServiceImpl(ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setTargetTemperatureState(TemperatureState temperatureState) throws CouldNotPerformException {
        return executeCommand(TemperatureStateTransformer.transform(temperatureState));
    }

    @Override
    public TemperatureState getTargetTemperatureState() throws NotAvailableException {
        return unit.getTargetTemperatureState();
    }
}
