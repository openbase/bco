/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author mpohling
 */
public interface UnitRemoteFactoryInterface {

    /**
     * Creates and initializes an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type and is used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    public DALRemoteService createAndInitUnitRemote(final UnitConfigType.UnitConfig config) throws CouldNotPerformException;

    /**
     * Creates an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     */
    public DALRemoteService createUnitRemote(final UnitConfigType.UnitConfig config) throws CouldNotPerformException;
    
}
