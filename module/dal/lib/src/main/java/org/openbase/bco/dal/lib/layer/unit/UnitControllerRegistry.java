package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.storage.registry.SynchronizableRegistry;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * 
 * @param <CONTROLLER> the builder used to build the unit data instances.
 */
public interface UnitControllerRegistry<CONTROLLER extends UnitController<?,?>> extends SynchronizableRegistry<String, CONTROLLER>, Activatable {

    /**
     * Returns a unit controller instance with the given alias.
     *
     * @param alias the alias used for identification.
     * @return the controller instance associated with the given alias.
     * @throws NotAvailableException is thrown in case there is no unit with the given alias available.
     */
    CONTROLLER getUnitByAlias(final String alias) throws NotAvailableException;
}
