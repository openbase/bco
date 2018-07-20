package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.operation.OperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Future;

/**
 * @param <M>  Underling message type.
 * @param <MB> Message related builder.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDALUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends AbstractUnitController<M, MB> implements OperationServiceFactoryProvider {

    private Map<ServiceType, OperationService> operationServiceMap;

    private final UnitHost unitHost;
    private final OperationServiceFactory operationServiceFactory;

    public AbstractDALUnitController(final Class unitClass, final UnitHost unitHost, final MB builder) throws InstantiationException {
        super(unitClass, builder);
        try {
            if (unitHost.getOperationServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }
            this.operationServiceMap = new TreeMap<>();
            this.unitHost = unitHost;
            this.operationServiceFactory = unitHost.getOperationServiceFactory();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);

        final Set<ServiceType> registeredServiceTypes = new HashSet<>(operationServiceMap.keySet());
        try {
            for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {

                // filter non operation services
                if (serviceDescription.getPattern() != ServicePattern.OPERATION) {
                    continue;
                }

                // filter already handled services
                if (registeredServiceTypes.contains(serviceDescription.getServiceType())) {
                    registeredServiceTypes.remove(serviceDescription.getServiceType());
                    continue;
                }

                operationServiceMap.put(serviceDescription.getServiceType(), getOperationServiceFactory().newInstance(serviceDescription.getServiceType(), this));
            }

            // remove deleted services
            for (final ServiceType outdatedServiceType : registeredServiceTypes) {
                operationServiceMap.remove(outdatedServiceType);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        return operationServiceFactory;
    }

    public UnitHost getUnitHost() {
        return unitHost;
    }

    @Override
    public Future<Void> performOperationService(final Message serviceState, final ServiceType serviceType) {
        //logger.debug("Set " + getUnitType().name() + "[" + getLabel() + "] to PowerState [" + serviceState + "]");
        try {
            Services.verifyOperationServiceState(serviceState);
            return (Future<Void>) Services.invokeOperationServiceMethod(serviceType, operationServiceMap.get(serviceType), serviceState);
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Void.class, ex);
        }
    }
}
