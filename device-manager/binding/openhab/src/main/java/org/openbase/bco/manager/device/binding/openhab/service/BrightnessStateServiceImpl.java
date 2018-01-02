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
import org.openbase.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.BrightnessStateType.BrightnessState;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <UNIT> Related unit.
 */
public class BrightnessStateServiceImpl<UNIT extends BrightnessStateOperationService & Unit<?>> extends OpenHABService<UNIT> implements BrightnessStateOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrightnessStateServiceImpl.class);
    
    public BrightnessStateServiceImpl(final UNIT unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        return unit.getBrightnessState();
    }

    @Override
    public Future<ActionFuture> setBrightnessState(final BrightnessState brightnessState) throws CouldNotPerformException {
        return executeCommand(OpenHABCommandFactory.newPercentCommand(brightnessState.getBrightness()));
    }
}
