package org.openbase.bco.registry.scene.core;

/*
 * #%L
 * BCO Registry Scene Core
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
import org.openbase.bco.registry.lib.launch.AbstractLauncher;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.bco.registry.scene.lib.SceneRegistry;
import org.openbase.bco.registry.scene.lib.jp.JPSceneRegistryScope;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPReadOnly;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SceneRegistryLauncher extends AbstractRegistryLauncher<SceneRegistryController> {

    public SceneRegistryLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(SceneRegistry.class, SceneRegistryController.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPSceneRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
    }

    public static void main(String args[]) throws Throwable {
        AbstractLauncher.main(args, SceneRegistry.class, SceneRegistryLauncher.class);
    }
}
