package org.openbase.bco.manager.location.binding.openhab;

/*
 * #%L
 * BCO Manager Location Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.remote.unit.connection.ConnectionRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote;
import static org.openbase.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import org.openbase.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.openbase.jul.extension.openhab.binding.transform.OpenhabCommandTransformer;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ActivatableEntryRegistrySynchronizer;
import org.openbase.jul.storage.registry.RegistryImpl;
import rst.domotic.binding.openhab.OpenhabCommandType.OpenhabCommand;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 */
public class LocationBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String LOCATION_MANAGER_ITEM_FILTER = "bco.manager.location";

    private final LocationRegistryRemote locationRegistryRemote;
    private final LocationRemoteFactoryImpl locationRemoteFactory;
    private final ConnectionRemoteFactoryImpl connectionRemoteFactory;
    private final ActivatableEntryRegistrySynchronizer<String, LocationRemote, UnitConfig, UnitConfig.Builder> locationRegistrySynchronizer;
    private final ActivatableEntryRegistrySynchronizer<String, ConnectionRemote, UnitConfig, UnitConfig.Builder> connectionRegistrySynchronizer;
    private final RegistryImpl<String, LocationRemote> locationRegistry;
    private final RegistryImpl<String, ConnectionRemote> connectionRegistry;
    private final boolean hardwareSimulationMode;
    private boolean active;

    public LocationBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();
        locationRegistryRemote = new LocationRegistryRemote();
        locationRegistry = new RegistryImpl<>();
        connectionRegistry = new RegistryImpl<>();
        locationRemoteFactory = new LocationRemoteFactoryImpl();
        connectionRemoteFactory = new ConnectionRemoteFactoryImpl();
        this.locationRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, LocationRemote, UnitConfig, UnitConfig.Builder>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), locationRemoteFactory) {

            @Override
            public boolean activationCondition(UnitConfig config) {
                return true;
            }
        };
        this.connectionRegistrySynchronizer = new ActivatableEntryRegistrySynchronizer<String, ConnectionRemote, UnitConfig, UnitConfig.Builder>(connectionRegistry, locationRegistryRemote.getConnectionConfigRemoteRegistry(), connectionRemoteFactory) {

            @Override
            public boolean activationCondition(UnitConfig config) {
                return true;
            }
        };
    }

    public void init() throws InitializationException, InterruptedException {
        init(LOCATION_MANAGER_ITEM_FILTER, new AbstractOpenHABRemote(hardwareSimulationMode) {

            @Override
            public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
                logger.debug("Ignore update for location manager openhab binding.");
            }

            @Override
            public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
                //TODO:paramite; compare this to the implementation in the device manager openhab binding
                try {
                    RSBRemoteService remote = null;
                    if (command.getItem().startsWith("Location")) {
                        logger.debug("Received command for location [" + command.getItem() + "] from openhab");
                        remote = locationRegistry.get(getIdFromOpenHABCommand(command));
                    } else if (command.getItem().startsWith("Connection")) {
                        logger.debug("Received command for connection [" + command.getItem() + "] from openhab");
                        remote = connectionRegistry.get(getIdFromOpenHABCommand(command));
                    }

                    if (remote == null) {
                        throw new NotAvailableException("No remote for item [" + command.getItem() + "] found");
                    }

                    Future returnValue;
                    ServiceType serviceType = getServiceTypeForCommand(command);
                    String methodName = "set" + StringProcessor.transformUpperCaseToCamelCase(serviceType.name()).replaceAll("Provider", "").replaceAll("Service", "");
                    Object serviceData = OpenhabCommandTransformer.getServiceData(command, serviceType);
                    Method relatedMethod;
                    try {
                        relatedMethod = remote.getClass().getMethod(methodName, serviceData.getClass());
                        if (relatedMethod == null) {
                            throw new NotAvailableException(relatedMethod);
                        }
                    } catch (NoSuchMethodException | SecurityException | NotAvailableException ex) {
                        throw new NotAvailableException("Method " + remote + "." + methodName + "(" + serviceData.getClass() + ")", ex);
                    }

                    try {
                        returnValue = (Future) relatedMethod.invoke(remote, serviceData);
                    } catch (IllegalAccessException ex) {
                        throw new CouldNotPerformException("Cannot access related Method [" + relatedMethod.getName() + "]", ex);
                    } catch (IllegalArgumentException ex) {
                        throw new CouldNotPerformException("Does not match [" + relatedMethod.getParameterTypes()[0].getName() + "] which is needed by [" + relatedMethod.getName() + "]!", ex);
                    } catch (InvocationTargetException ex) {
                        throw new CouldNotPerformException("The related method [" + relatedMethod.getName() + "] throws an exception during invocation!", ex);
                    }

                    GlobalCachedExecutorService.applyErrorHandling(returnValue, (Exception input) -> {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Waiting for result on method failed with exception", input), logger);
                        return null;
                    }, 30, TimeUnit.SECONDS);
                } catch (NotAvailableException ex) {
                    throw new CouldNotPerformException(ex);
                }
            }
        });
    }

    private String getIdFromOpenHABCommand(OpenhabCommand command) {
        return command.getItemBindingConfig().split(":")[1];
    }

    private ServiceType getServiceTypeForCommand(OpenhabCommand command) {
        return ServiceType.valueOf(StringProcessor.transformToUpperCase(command.getItem().split(ITEM_SEGMENT_DELIMITER)[1]));
    }

    @Override
    public void init(String itemFilter, OpenHABRemote openHABRemote) throws InitializationException, InterruptedException {
        super.init(itemFilter, openHABRemote);
        try {
            locationRemoteFactory.init(openHABRemote);
            locationRegistryRemote.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
        locationRegistryRemote.activate();
        locationRegistrySynchronizer.activate();
        connectionRegistrySynchronizer.activate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
        locationRegistryRemote.deactivate();
        locationRegistrySynchronizer.deactivate();
        connectionRegistrySynchronizer.deactivate();
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
