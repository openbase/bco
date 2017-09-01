package org.openbase.bco.dal.lib.layer.service.collection;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.operation.EmphasisStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.EmphasisStateType.EmphasisState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public interface EmphasisStateOperationServiceCollection extends EmphasisStateOperationService {

    public Future<ActionFuture> setEmphasisState(final EmphasisState emphasisState, final UnitType unitType) throws CouldNotPerformException;

    /**
     * Returns the average emphasis value for a collection of brightnessServices.
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public EmphasisState getEmphasisState() throws NotAvailableException;

    /**
     * Returns the average emphasis value for a collection of brightnessServices with given unitType.
     *
     * @param unitType
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    public EmphasisState getEmphasisState(final UnitType unitType) throws NotAvailableException;

}
