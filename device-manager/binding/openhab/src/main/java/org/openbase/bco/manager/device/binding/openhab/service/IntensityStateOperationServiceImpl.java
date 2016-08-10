/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.manager.device.binding.openhab.service;

import java.util.concurrent.Future;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.bco.dal.lib.layer.service.operation.IntensityStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.homeautomation.state.IntensityStateType.IntensityState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class IntensityStateOperationServiceImpl<UNIT extends IntensityStateOperationService & Unit> extends OpenHABService<UNIT> implements IntensityStateOperationService {

    public IntensityStateOperationServiceImpl(final UNIT unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public Future<Void> setIntensityState(IntensityState intensityState) throws CouldNotPerformException {
        return executeCommand(OpenHABCommandFactory.newPercentCommand(intensityState.getIntensity()));
    }

    @Override
    public IntensityState getIntensityState() throws NotAvailableException {
        return unit.getIntensityState();
    }
}
