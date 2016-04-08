/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.scene.core;

/*
 * #%L
 * COMA SceneManager Core
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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
import org.dc.bco.manager.scene.lib.SceneController;
import org.dc.bco.manager.scene.lib.SceneFactory;
import org.dc.bco.manager.scene.lib.SceneManager;
import org.dc.bco.registry.scene.lib.provider.SceneRegistryProvider;
import org.dc.bco.registry.scene.remote.SceneRegistryRemote;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.storage.registry.EnableableEntryRegistrySynchronizer;
import org.dc.jul.storage.registry.RegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.state.EnablingStateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class SceneManagerController implements SceneRegistryProvider, SceneManager {

    protected static final Logger logger = LoggerFactory.getLogger(SceneManagerController.class);

    private static SceneManagerController instance;
    private final SceneFactory factory;
    private final RegistryImpl<String, SceneController> sceneRegistry;
    private final SceneRegistryRemote sceneRegistryRemote;
    private final EnableableEntryRegistrySynchronizer<String, SceneController, SceneConfig, SceneConfig.Builder> registrySynchronizer;

    public SceneManagerController() throws org.dc.jul.exception.InstantiationException, InterruptedException {
        try {
            this.instance = this;
            this.factory = SceneFactoryImpl.getInstance();
            this.sceneRegistry = new RegistryImpl<>();
            this.sceneRegistryRemote = new SceneRegistryRemote();

            this.registrySynchronizer = new EnableableEntryRegistrySynchronizer<String, SceneController, SceneConfig, SceneConfig.Builder>(sceneRegistry, sceneRegistryRemote.getSceneConfigRemoteRegistry(), factory) {

                @Override
                public boolean enablingCondition(SceneConfig config) {
                    return config.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED);
                }
            };
        } catch (CouldNotPerformException ex) {
            throw new org.dc.jul.exception.InstantiationException(this, ex);
        }
    }

    public static SceneManagerController getInstance() throws NotAvailableException {
        if (instance == null) {
            throw new NotAvailableException(SceneManagerController.class);
        }
        return instance;
    }

    public void init() throws InitializationException, InterruptedException {
        try {
            this.sceneRegistryRemote.init();
            this.sceneRegistryRemote.activate();
            this.registrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void shutdown() {
        sceneRegistryRemote.shutdown();
        registrySynchronizer.shutdown();
        instance = null;
    }

    @Override
    public org.dc.bco.registry.scene.lib.SceneRegistry getSceneRegistry() throws NotAvailableException {
        return sceneRegistryRemote;
    }

    public RegistryImpl<String, SceneController> getSceneControllerRegistry() {
        return sceneRegistry;
    }
}
