package org.openbase.bco.registry.provider;

/*
 * #%L
 * REM Utility
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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

import org.openbase.bco.registry.agent.lib.provider.AgentRegistryProvider;
import org.openbase.bco.registry.app.lib.provider.AppRegistryProvider;
import org.openbase.bco.registry.device.lib.provider.DeviceRegistryProvider;
import org.openbase.bco.registry.location.lib.provider.LocationRegistryProvider;
import org.openbase.bco.registry.scene.lib.provider.SceneRegistryProvider;
import org.openbase.bco.registry.scene.lib.provider.UnitRegistryProvider;
import org.openbase.bco.registry.user.lib.provider.UserRegistryProvider;

/**
 * Interface provides a collection of globally managed registry instances.
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface RegistryProvider extends UnitRegistryProvider, AgentRegistryProvider, AppRegistryProvider, DeviceRegistryProvider, LocationRegistryProvider, SceneRegistryProvider, UserRegistryProvider {

}
