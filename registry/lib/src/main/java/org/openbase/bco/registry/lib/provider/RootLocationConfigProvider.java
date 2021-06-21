package org.openbase.bco.registry.lib.provider;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public interface RootLocationConfigProvider {
    /**
     * Method returns the root location of the registered location hierarchy
     * tree.
     *
     * @return the root location
     * @throws CouldNotPerformException is thrown if the request fails.
     * @throws NotAvailableException is thrown if no rood connection exists.
     */
    UnitConfig getRootLocationConfig() throws CouldNotPerformException, NotAvailableException;

}
