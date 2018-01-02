package org.openbase.bco.manager.scene.core;

/*
 * #%L
 * BCO Manager Scene Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.manager.scene.lib.SceneManager;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import static org.openbase.jul.pattern.launch.AbstractLauncher.main;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class SceneManagerLauncher extends AbstractLauncher<SceneManagerController> {

    public SceneManagerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(SceneManager.class, SceneManagerController.class);
    }

    @Override
    public void loadProperties() {
    }

    public static void main(String args[]) throws Throwable {
        BCO.printLogo();
        main(args, SceneManager.class, SceneManagerLauncher.class);
    }
}
