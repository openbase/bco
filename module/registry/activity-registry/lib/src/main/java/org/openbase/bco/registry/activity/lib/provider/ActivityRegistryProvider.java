package org.openbase.bco.registry.activity.lib.provider;

/*
 * #%L
 * BCO Registry Activity Library
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
import org.openbase.bco.registry.activity.lib.ActivityRegistry;
import org.openbase.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed activity registry instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface ActivityRegistryProvider {

    /**
     * Get the globally managed activity registry instance.
     *
     * @return The globally managed activity registry instance.
     * @throws NotAvailableException if the activity registry is not available
     */
    ActivityRegistry getActivityRegistry() throws NotAvailableException;
}
