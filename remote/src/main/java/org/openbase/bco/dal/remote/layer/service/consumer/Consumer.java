package org.openbase.bco.dal.remote.layer.service.consumer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.ServiceRemote;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.bco.dal.remote.layer.service.ServiceRemoteFactoryImpl;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotInitializedException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class Consumer implements Manageable<ServiceConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private boolean active;
    private ServiceRemote boundedProviderService;
    private final Observer serviceStateObserver;

    public Consumer(final UnitController<?,?> unitController) {
        this.active = false;
        this.serviceStateObserver = (source, data) -> {
            try {
                ActionDescription responsibleAction = Services.getResponsibleAction(((Message) data));
                
                // build consumer action out of responsible action
                ActionDescription.Builder consumerActionBuilder = responsibleAction.toBuilder();
                consumerActionBuilder.getServiceStateDescriptionBuilder().setUnitId(unitController.getId());
                consumerActionBuilder.getServiceStateDescriptionBuilder().setUnitType(unitController.getUnitType());
                
                // apply new action
                unitController.applyAction(consumerActionBuilder.build());
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not consume update!", ex, LOGGER);
            }
        };
    }

    /**
     * Initializes this consumer service and bounds those to the given provider service referred by the given provider service configuration.
     *
     * @param providerServiceConfig the config of the provider service to connect to.
     * @throws InitializationException is thrown if the initialization fails.
     * @throws InterruptedException is thrown if the current thread was externally interrupted.
     */
    @Override
    public synchronized void init(final ServiceConfig providerServiceConfig) throws InitializationException, InterruptedException {
        try {
            if (boundedProviderService != null) {
                boundedProviderService.shutdown();
            }
            boundedProviderService = ServiceRemoteFactoryImpl.getInstance().newInitializedInstanceById(providerServiceConfig.getServiceDescription().getServiceType(), providerServiceConfig.getUnitId());
        } catch (final CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @throws CouldNotPerformException {@inheritDoc }
     * @throws InterruptedException {@inheritDoc }
     */
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        try {
            validateInitialization();
            active = true;
            boundedProviderService.addServiceStateObserver(boundedProviderService.getServiceType(), serviceStateObserver);
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not activate " + this, ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @throws CouldNotPerformException {@inheritDoc }
     * @throws InterruptedException {@inheritDoc }
     */
    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        boundedProviderService.removeServiceStateObserver(boundedProviderService.getServiceType(), serviceStateObserver);
        active = false;
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     */
    @Override
    public boolean isActive() {
        return active;
    }

    /**
     *
     * @throws NotInitializedException
     */
    public void validateInitialization() throws NotInitializedException {
        if (boundedProviderService == null) {
            throw new NotInitializedException(this);
        }
    }
}
