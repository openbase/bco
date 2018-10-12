package org.openbase.bco.manager.location.lib;

import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.storage.registry.RegistryImpl;

/*
 * #%L
 * BCO Manager Location Library
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface LocationManager {

    /**
     * Enables access of the controller registry of this manager.
     * <p>
     * Note: Mainly used for accessing the controller via test routines.
     *
     * @return the controller registry.
     *
     * @throws NotAvailableException is thrown if the controller registry is not available.
     */
    UnitControllerRegistry<LocationController> getLocationControllerRegistry();

    /**
     * Enables access of the controller registry of this manager.
     * <p>
     * Note: Mainly used for accessing the controller via test routines.
     *
     * @return the controller registry.
     *
     * @throws NotAvailableException is thrown if the controller registry is not available.
     */
    UnitControllerRegistry<ConnectionController> getConnectionControllerRegistry();
}
