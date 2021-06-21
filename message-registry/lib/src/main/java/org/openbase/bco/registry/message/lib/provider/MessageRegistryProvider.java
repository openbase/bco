package org.openbase.bco.registry.message.lib.provider;

/*
 * #%L
 * BCO Registry Message Library
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
import org.openbase.bco.registry.message.lib.MessageRegistry;
import org.openbase.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed message registry instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface MessageRegistryProvider {

    /**
     * Returns the globally managed message registry instance.
     *
     * @return a registry instance.
     * @throws NotAvailableException is thrown if the registry is not available.
     */
    MessageRegistry getMessageRegistry() throws NotAvailableException;
}
