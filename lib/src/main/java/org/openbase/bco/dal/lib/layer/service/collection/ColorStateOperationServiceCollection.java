package org.openbase.bco.dal.lib.layer.service.collection;

/*
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
import org.openbase.bco.dal.lib.layer.service.operation.ColorStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.ColorStateType.ColorState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 * * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.com">Marian Pohling</a>
 */
public interface ColorStateOperationServiceCollection extends ColorStateOperationService {

    public Future<Void> setColorState(final ColorState colorState, final UnitType unitType) throws CouldNotPerformException;

    /**
     * Computes the average RGB color for the underlying services.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    public ColorState getColorState() throws NotAvailableException;

    /**
     * Computes the average RGB color for the underlying services with given unitType.
     *
     * @param unitType
     * @return
     * @throws NotAvailableException
     */
    public ColorState getColorState(final UnitType unitType) throws NotAvailableException;
}
