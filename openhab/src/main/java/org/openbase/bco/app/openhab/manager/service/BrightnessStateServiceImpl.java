package org.openbase.bco.app.openhab.manager.service;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.openbase.bco.app.openhab.manager.transform.BrightnessStateTransformer;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.BrightnessStateType.BrightnessState;

import java.util.concurrent.Future;

public class BrightnessStateServiceImpl<ST extends BrightnessStateOperationService & Unit<?>> extends OpenHABService<ST> implements BrightnessStateOperationService {

    BrightnessStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<ActionFuture> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        return executeCommand(BrightnessStateTransformer.transform(brightnessState));
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        return unit.getBrightnessState();
    }
}
