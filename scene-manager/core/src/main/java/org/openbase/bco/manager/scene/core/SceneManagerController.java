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
import org.openbase.bco.manager.scene.lib.SceneController;
import org.openbase.bco.manager.scene.lib.SceneFactory;
import org.openbase.bco.manager.scene.lib.SceneManager;
import org.openbase.bco.registry.login.SystemLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ControllerRegistryImpl;
import org.openbase.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.state.EnablingStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneManagerController implements SceneManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SceneManagerController.class);

    private final SceneFactory factory;
    private final ControllerRegistryImpl<String, SceneController> sceneRegistry;
    private final EnableableEntryRegistrySynchronizer<String, SceneController, UnitConfig, UnitConfig.Builder> sceneRegistrySynchronizer;

    public SceneManagerController() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.factory = SceneFactoryImpl.getInstance();
            this.sceneRegistry = new ControllerRegistryImpl<>();

            this.sceneRegistrySynchronizer = new EnableableEntryRegistrySynchronizer<String, SceneController, UnitConfig, UnitConfig.Builder>(sceneRegistry, Registries.getSceneRegistry().getSceneConfigRemoteRegistry(), Registries.getSceneRegistry(), factory) {

                @Override
                public boolean enablingCondition(UnitConfig config) {
                    return config.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED);
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        // This has to stay. Else do not implement VoidInitializable. 
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        //TODO: why is this necessary
        Registries.waitForData();
        SystemLogin.loginBCOUser();
        sceneRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return sceneRegistrySynchronizer.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        sceneRegistrySynchronizer.deactivate();
    }

    @Override
    public void shutdown() {
        sceneRegistrySynchronizer.shutdown();
    }

    public RegistryImpl<String, SceneController> getSceneControllerRegistry() {
        return sceneRegistry;
    }
}
