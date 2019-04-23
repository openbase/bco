package org.openbase.bco.dal.control.layer.unit.scene;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.dal.control.layer.unit.UnitControllerRegistrySynchronizer;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistry;
import org.openbase.bco.dal.lib.layer.unit.UnitControllerRegistryImpl;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneController;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneControllerFactory;
import org.openbase.bco.dal.lib.layer.unit.scene.SceneManager;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneManagerImpl implements SceneManager, Launchable<Void>, VoidInitializable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SceneManagerImpl.class);

    private final SceneControllerFactory factory;
    private final UnitControllerRegistry<SceneController> sceneControllerRegistry;
    private final UnitControllerRegistrySynchronizer<SceneController> sceneRegistrySynchronizer;

    public SceneManagerImpl() throws org.openbase.jul.exception.InstantiationException {
        try {
            this.factory = SceneControllerFactoryImpl.getInstance();
            this.sceneControllerRegistry = new UnitControllerRegistryImpl<>();
            this.sceneRegistrySynchronizer = new UnitControllerRegistrySynchronizer<>(sceneControllerRegistry, Registries.getUnitRegistry().getSceneUnitConfigRemoteRegistry(), factory);
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
        BCOLogin.getSession().loginBCOUser();
        sceneControllerRegistry.activate();
        sceneRegistrySynchronizer.activate();
    }

    @Override
    public boolean isActive() {
        return sceneRegistrySynchronizer.isActive() &&
                sceneControllerRegistry.isActive();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        sceneRegistrySynchronizer.deactivate();
        sceneControllerRegistry.deactivate();
    }

    @Override
    public void shutdown() {
        sceneRegistrySynchronizer.shutdown();
        sceneControllerRegistry.shutdown();
    }

    @Override
    public UnitControllerRegistry<SceneController> getSceneControllerRegistry() {
        return sceneControllerRegistry;
    }
}
