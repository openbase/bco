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
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.manager.scene.lib.SceneController;
import org.openbase.bco.manager.scene.lib.SceneControllerFactory;
import org.openbase.bco.manager.scene.lib.SceneManager;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneManagerImpl implements SceneManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SceneManagerImpl.class);

    private final SceneControllerFactory factory;
    private final UnitControllerRegistry<SceneController> sceneControllerRegistry;
    private final ActivatableEntryRegistrySynchronizer<String, SceneController, UnitConfig, Builder> sceneRegistrySynchronizer;

    public SceneManagerImpl() throws org.openbase.jul.exception.InstantiationException, InterruptedException {
        try {
            this.factory = SceneControllerFactoryImpl.getInstance();
            this.sceneControllerRegistry = new UnitControllerRegistryImpl<>();

            this.sceneRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, SceneController, UnitConfig, UnitConfig.Builder>(sceneControllerRegistry, Registries.getUnitRegistry().getSceneUnitConfigRemoteRegistry(), Registries.getUnitRegistry(), factory) {

                @Override
                public boolean activationCondition(UnitConfig config) {
                    return UnitConfigProcessor.isEnabled(config);
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(this, ex);
        }
    }

    @Override
    public void init() {
        // This has to stay. Else do not implement VoidInitializable. 
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        BCOLogin.loginBCOUser();
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

    @Override
    public UnitControllerRegistry<SceneController> getSceneControllerRegistry() {
        return sceneControllerRegistry;
    }
}
