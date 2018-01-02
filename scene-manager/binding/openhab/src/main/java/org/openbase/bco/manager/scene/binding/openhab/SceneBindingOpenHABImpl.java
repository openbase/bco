package org.openbase.bco.manager.scene.binding.openhab;

/*
 * #%L
 * BCO Manager Scene Binding OpenHAB
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
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.scene.SceneRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.storage.registry.RegistrySynchronizer;
import org.openbase.jul.storage.registry.RemoteControllerRegistryImpl;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class SceneBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String SCENE_MANAGER_ITEM_FILTER = "bco.manager.scene";

    private final SceneRemoteFactoryImpl factory;
    private final RegistrySynchronizer<String, SceneRemote, UnitConfig, UnitConfig.Builder> registrySynchronizer;
    private final RemoteControllerRegistryImpl<String, SceneRemote> registry;
    private final boolean hardwareSimulationMode;
    private boolean active;

    public SceneBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        try {
            registry = new RemoteControllerRegistryImpl<>();
            factory = new SceneRemoteFactoryImpl();
            hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();

            this.registrySynchronizer = new RegistrySynchronizer<String, SceneRemote, UnitConfig, UnitConfig.Builder>(registry, Registries.getSceneRegistry().getSceneConfigRemoteRegistry(), Registries.getSceneRegistry(), factory) {

                @Override
                public boolean verifyConfig(final UnitConfig config) throws VerificationFailedException {
                    return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
                }
            };
        } catch (final CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        super.init(SCENE_MANAGER_ITEM_FILTER, new SceneBindingOpenHABRemote(hardwareSimulationMode, registry));
        try {
            Registries.getSceneRegistry().waitForData();
            factory.init(openHABRemote);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        registrySynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        registrySynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
