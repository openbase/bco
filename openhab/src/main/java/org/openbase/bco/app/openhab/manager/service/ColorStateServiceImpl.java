package org.openbase.bco.app.openhab.manager.service;

import org.openbase.bco.app.openhab.manager.transform.ColorStateTransformer;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.ColorStateType.ColorState;

import java.util.concurrent.Future;

public class ColorStateServiceImpl<ST extends ColorStateOperationService & Unit<?>> extends OpenHABService<ST> implements ColorStateOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorStateServiceImpl.class);

//    private final boolean autoRepeat;

    ColorStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
//        this.autoRepeat = ServiceFactoryTools.detectAutoRepeat(unit);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        return unit.getColorState();
    }

    @Override
    public Future<ActionFuture> setColorState(final ColorState colorState) throws CouldNotPerformException {
        return executeCommand(ColorStateTransformer.transform(colorState));
    }
}
