package org.openbase.bco.registry.scene.lib.provider;

/*
 * #%L
 * BCO Registry Scene Library
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

import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.jul.exception.NotAvailableException;

/**
 * Interface provides a globally managed scene registry instance.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface SceneRegistryProvider {

    /**
     * Returns the globally managed scene registry instance.
     * @return
     * @throws NotAvailableException 
     */
    public SceneRegistry getSceneRegistry() throws NotAvailableException;
}
