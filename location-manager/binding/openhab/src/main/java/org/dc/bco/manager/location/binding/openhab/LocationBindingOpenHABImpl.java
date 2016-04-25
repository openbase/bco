package org.dc.bco.manager.location.binding.openhab;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.dc.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.dc.bco.manager.location.remote.ConnectionRemote;
import org.dc.bco.manager.location.remote.LocationRemote;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABBinding;
import org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote;
import static org.dc.jul.extension.openhab.binding.AbstractOpenHABRemote.ITEM_SEGMENT_DELIMITER;
import org.dc.jul.extension.openhab.binding.interfaces.OpenHABRemote;
import org.dc.jul.extension.openhab.binding.transform.OpenhabCommandTransformer;
import org.dc.jul.extension.rsb.com.RSBRemoteService;
import org.dc.jul.processing.StringProcessor;
import org.dc.jul.storage.registry.RegistryImpl;
import org.dc.jul.storage.registry.RegistrySynchronizer;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.spatial.ConnectionConfigType.ConnectionConfig;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine
 * Threepwood</a>
 */
public class LocationBindingOpenHABImpl extends AbstractOpenHABBinding {

    //TODO: Should be declared in the openhab config generator and used from there
    public static final String LOCATION_MANAGER_ITEM_FILTER = "bco.manager.location";

    private final LocationRegistryRemote locationRegistryRemote;
    private final LocationRemoteFactoryImpl locationRemoteFactory;
    private final ConnectionRemoteFactoryImpl connectionRemoteFactory;
    private final RegistrySynchronizer<String, LocationRemote, LocationConfig, LocationConfig.Builder> locationRegistrySynchronizer;
    private final RegistrySynchronizer<String, ConnectionRemote, ConnectionConfig, ConnectionConfig.Builder> connectionRegistrySynchronizer;
    private final RegistryImpl<String, LocationRemote> locationRegistry;
    private final RegistryImpl<String, ConnectionRemote> connectionRegistry;
    private final boolean hardwareSimulationMode;

    public LocationBindingOpenHABImpl() throws InstantiationException, JPNotAvailableException, InterruptedException {
        super();
        hardwareSimulationMode = JPService.getProperty(JPHardwareSimulationMode.class).getValue();
        locationRegistryRemote = new LocationRegistryRemote();
        locationRegistry = new RegistryImpl<>();
        connectionRegistry = new RegistryImpl<>();
        locationRemoteFactory = new LocationRemoteFactoryImpl();
        connectionRemoteFactory = new ConnectionRemoteFactoryImpl();
        this.locationRegistrySynchronizer = new RegistrySynchronizer<>(locationRegistry, locationRegistryRemote.getLocationConfigRemoteRegistry(), locationRemoteFactory);
        this.connectionRegistrySynchronizer = new RegistrySynchronizer<>(connectionRegistry, locationRegistryRemote.getConnectionConfigRemoteRegistry(), connectionRemoteFactory);
    }

    public void init() throws InitializationException, InterruptedException {
        init(LOCATION_MANAGER_ITEM_FILTER, new AbstractOpenHABRemote(hardwareSimulationMode) {

            @Override
            public void internalReceiveUpdate(OpenhabCommand command) throws CouldNotPerformException {
                logger.debug("Ignore update for location manager openhab binding.");
            }

            @Override
            public void internalReceiveCommand(OpenhabCommand command) throws CouldNotPerformException {
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
                        relatedMethod.invoke(remote, serviceData);
                    } catch (IllegalAccessException ex) {
                        throw new CouldNotPerformException("Cannot access related Method [" + relatedMethod.getName() + "]", ex);
                    } catch (IllegalArgumentException ex) {
                        throw new CouldNotPerformException("Does not match [" + relatedMethod.getParameterTypes()[0].getName() + "] which is needed by [" + relatedMethod.getName() + "]!", ex);
                    } catch (InvocationTargetException ex) {
                        throw new CouldNotPerformException("The related method [" + relatedMethod.getName() + "] throws an exceptioin during invocation!", ex);
                    }
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
            locationRegistryRemote.init();
            locationRegistryRemote.activate();
            locationRegistrySynchronizer.init();
            connectionRegistrySynchronizer.init();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }
}
