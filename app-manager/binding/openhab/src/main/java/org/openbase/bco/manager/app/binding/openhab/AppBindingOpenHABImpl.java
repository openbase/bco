package org.openbase.bco.manager.app.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import org.openbase.bco.dal.remote.unit.app.AppRemote;
import org.openbase.bco.manager.app.binding.openhab.transform.ActivationStateTransformer;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.storage.registry.RegistryImpl;
import org.openbase.jul.storage.registry.RegistrySynchronizer;
import rst.domotic.binding.openhab.OpenhabCommandType.OpenhabCommand;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class AppBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String AGENT_MANAGER_ITEM_FILTER = "bco.manager.app";

    private final AppRegistryRemote appRegistryRemote;
    private final AppRemoteFactoryImpl factory;
    private final RegistrySynchronizer<String, AppRemote, UnitConfig, UnitConfig.Builder> registrySynchronizer;
    private final RegistryImpl<String, AppRemote> registry;
    private final boolean hardwareSimulationMode;

    public AppBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        appRegistryRemote = new AppRegistryRemote();
        registry = new RegistryImpl<>();
        factory = new AppRemoteFactoryImpl();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();

        this.registrySynchronizer = new RegistrySynchronizer<String, AppRemote, UnitConfig, UnitConfig.Builder>(registry, appRegistryRemote.getAppConfigRemoteRegistry(), factory) {

            @Override
            public boolean verifyConfig(final UnitConfig config) throws VerificationFailedException {
                return config.getEnablingState().getValue() == EnablingState.State.ENABLED;
            }
        };
    }

    private String getIdFromOpenHABItem(OpenhabCommand command) {
        return command.getItemBindingConfig().split(":")[1];
    }

    public void init() throws InitializationException, InterruptedException {
        init(AGENT_MANAGER_ITEM_FILTER, new AbstractOpenHABRemote(hardwareSimulationMode) {

            @Override
            public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
                logger.debug("Ignore update for app manager openhab binding.");
            }

            @Override
            public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
                try {

                    if (!command.hasOnOff() || !command.getOnOff().hasState()) {
                        throw new CouldNotPerformException("Command does not have an onOff value required for apps");
                    }
                    logger.debug("Received command for app [" + command.getItem() + "] from openhab");
                    registry.get(getIdFromOpenHABItem(command)).setActivationState(ActivationStateTransformer.transform(command.getOnOff().getState()));
                } catch (CouldNotPerformException ex) {
                    throw new CouldNotPerformException("Skip item update [" + command.getItem() + " = " + command.getOnOff() + "]!", ex);
                }
            }
        });
    }

    @Override
    public void init(String itemFilter, OpenHABRemote openHABRemote) throws InitializationException, InterruptedException {
        super.init(itemFilter, openHABRemote);
        try {
            factory.init(openHABRemote);
            appRegistryRemote.init();
            appRegistryRemote.activate();
            registrySynchronizer.activate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }
}
