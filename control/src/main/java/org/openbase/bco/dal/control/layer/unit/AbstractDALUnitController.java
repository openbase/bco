package org.openbase.bco.dal.control.layer.unit;

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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.unit.HostUnit;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @param <M>  Underling message type.
 * @param <MB> Message related builder.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDALUnitController<M extends AbstractMessage & Serializable, MB extends M.Builder<MB>> extends AbstractUnitController<M, MB> implements OperationServiceFactoryProvider {



    private final HostUnitController hostUnit;
    private final OperationServiceFactory operationServiceFactory;

    public AbstractDALUnitController(final Class unitClass, final HostUnitController hostUnitController, final MB builder) throws InstantiationException {
        super(unitClass, builder);
        try {
            if (hostUnitController.getOperationServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }
            this.hostUnit = hostUnitController;
            this.operationServiceFactory = hostUnitController.getOperationServiceFactory();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);

        final Set<ServiceType> registeredServiceTypes = new HashSet<>(getOperationServiceMap().keySet());
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

                registerOperationService(serviceDescription.getServiceType(), getOperationServiceFactory().newInstance(serviceDescription.getServiceType(), this));
            }

            // remove deleted services
            for (final ServiceType outdatedServiceType : registeredServiceTypes) {
                removeOperationService(outdatedServiceType);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        return operationServiceFactory;
    }

    public HostUnit getHostUnit() {
        return hostUnit;
    }
}
