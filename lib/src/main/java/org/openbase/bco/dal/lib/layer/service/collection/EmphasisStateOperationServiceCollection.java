package org.openbase.bco.dal.lib.layer.service.collection;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import java.util.concurrent.Future;

import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.EmphasisStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public interface EmphasisStateOperationServiceCollection extends EmphasisStateOperationService {

    public Future<ActionDescription> setEmphasisState(final EmphasisState emphasisState, final UnitType unitType) throws CouldNotPerformException;

    /**
     * Returns the average emphasis value for a collection of brightnessServices.
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    default EmphasisState getEmphasisState() throws NotAvailableException {
        return EmphasisStateOperationService.super.getEmphasisState();
    }

    /**
     * Returns the average emphasis value for a collection of brightnessServices with given unitType.
     *
     * @param unitType
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    EmphasisState getEmphasisState(final UnitType unitType) throws NotAvailableException;

}
