package org.openbase.bco.registry.location.core;

/*
 * #%L
 * BCO Registry Location Core
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import static org.openbase.bco.registry.lib.launch.AbstractLauncher.main;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.bco.registry.location.lib.LocationRegistry;
import org.openbase.bco.registry.location.lib.jp.JPLocationRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class LocationRegistryLauncher extends AbstractRegistryLauncher<LocationRegistryController> {

    public LocationRegistryLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(LocationRegistry.class, LocationRegistryController.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPLocationRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
    }

    public static void main(String args[]) throws Throwable {
        main(args, LocationRegistry.class, LocationRegistryLauncher.class);
    }
}
