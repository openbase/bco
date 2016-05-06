package org.dc.bco.manager.device.binding.openhab.service;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.manager.device.binding.openhab.execution.OpenHABCommandFactory;
import org.dc.bco.dal.lib.layer.service.operation.ColorOperationService;
import org.dc.bco.dal.lib.layer.unit.Unit;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import rst.vision.HSVColorType;

/**
 *
 * @author mpohling
 * @param <ST> Related service type.
 */
public class ColorServiceImpl<ST extends ColorOperationService & Unit>  extends OpenHABService<ST> implements ColorOperationService {

    public ColorServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public HSVColorType.HSVColor getColor() throws NotAvailableException {
        return unit.getColor();
    }

    @Override
    public Future<Void> setColor(HSVColorType.HSVColor color) throws CouldNotPerformException {
        return executeCommand(OpenHABCommandFactory.newHSBCommand(color));
    }
}
