package org.openbase.bco.dal.control.layer.unit;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactory;
import org.openbase.bco.dal.lib.layer.service.OperationServiceFactoryProvider;
import org.openbase.bco.dal.lib.layer.service.UnitDataSourceFactory;
import org.openbase.bco.dal.lib.layer.unit.HostUnit;
import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.iface.Activatable;
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
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractDALUnitController<M extends AbstractMessage & Serializable, MB extends M.Builder<MB>> extends AbstractUnitController<M, MB> implements OperationServiceFactoryProvider {

    private final HostUnitController hostUnit;
    private Activatable dataSource;

    public AbstractDALUnitController(final HostUnitController hostUnitController, final MB builder) throws InstantiationException {
        super(builder);
        try {
            if (hostUnitController.getOperationServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }
            this.hostUnit = hostUnitController;
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
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();

        // bind data source
        if(dataSource == null) {
            try {
                dataSource = hostUnit.getUnitDataSourceFactory().newInstance(this);
            } catch (NotAvailableException ex) {
                // not all unit require a data source.
            } catch (InstantiationException ex) {
                throw new InvalidStateException("Could not bind datasource!", ex);
            }
        }

        // activate datasource
        if(dataSource != null) {
            dataSource.activate();
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        super.deactivate();

        // deactivate data source
        if(dataSource != null) {
            dataSource.deactivate();
        }
    }

    @Override
    public OperationServiceFactory getOperationServiceFactory() throws NotAvailableException {
        return hostUnit.getOperationServiceFactory();
    }

    public HostUnit getHostUnit() {
        return hostUnit;
    }
}
