package org.openbase.bco.device.openhab.manager.service;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.vision.ColorType.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ColorStateType.ColorState;

import java.util.concurrent.Future;

// todo: class possibly not used anymore, please validate and cleanup.
@Deprecated
public class ColorStateServiceImpl<ST extends ColorStateOperationService & Unit<?>> extends OpenHABService<ST> implements ColorStateOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorStateServiceImpl.class);

    public ColorStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public ColorState getColorState() throws NotAvailableException {
        return unit.getColorState();
    }

    @Override
    public Future<ActionDescription> setColorState(final ColorState colorState) {
        return setState(colorState);
    }

    @Override
    public Color getNeutralWhiteColor() throws NotAvailableException {
        return unit.getNeutralWhiteColor();
    }
}
