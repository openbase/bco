package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.com">Marian Pohling</a>
 */
public interface ColorStateOperationServiceCollection extends ColorStateOperationService {

    default Future<ActionDescription> setColorState(final ColorState colorState, final UnitType unitType) {
        try {
            return getServiceProvider().applyAction(ActionDescriptionProcessor.generateDefaultActionParameter(colorState, ServiceType.COLOR_STATE_SERVICE, unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    /**
     * Computes the average RGB color for the underlying services.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Override
    default ColorState getColorState() throws NotAvailableException {
        return ColorStateOperationService.super.getColorState();
    }

    /**
     * Computes the average RGB color for the underlying services with given unitType.
     *
     * @param unitType
     *
     * @return
     *
     * @throws NotAvailableException
     */
    ColorState getColorState(final UnitType unitType) throws NotAvailableException;
}
