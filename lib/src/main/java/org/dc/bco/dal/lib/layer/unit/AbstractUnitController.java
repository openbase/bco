/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.data.Location;
import org.dc.bco.dal.lib.layer.service.Service;
import org.dc.bco.dal.lib.layer.service.ServiceFactory;
import org.dc.bco.dal.lib.layer.service.ServiceFactoryProvider;
import org.dc.bco.dal.lib.layer.service.ServiceType;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.extension.rsb.com.RSBCommunicationService;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.MultiException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.extension.rsb.iface.RSBLocalServerInterface;
import org.dc.jul.extension.rsb.scope.ScopeTransformer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate;

/**
 *
 * @author mpohling
 * @param <M> Underling message type.
 * @param <MB> Message related builder.
 */
public abstract class AbstractUnitController<M extends GeneratedMessage, MB extends M.Builder<MB>> extends RSBCommunicationService<M, MB> implements Unit, ServiceFactoryProvider {

    public final static String TYPE_FILED_ID = "id";
    public final static String TYPE_FILED_LABEL = "label";

    protected final UnitConfig config;
    protected final Location location;
    private final UnitHost unitHost;
    private final List<Service> serviceList;
    private final ServiceFactory serviceFactory;

    public AbstractUnitController(final UnitConfigType.UnitConfig config, final Class unitClass, final UnitHost unitHost, final MB builder) throws CouldNotPerformException {
        super(builder);
        try {
            if (config == null) {
                throw new NotAvailableException("config");
            }

            if (!config.hasId()) {
                throw new NotAvailableException("config.id");
            }

            if (config.getId().isEmpty()) {
                throw new NotAvailableException("Field config.id is empty!");
            }

            if (!config.hasLabel()) {
                throw new NotAvailableException("config.label");
            }

            if (config.getLabel().isEmpty()) {
                throw new NotAvailableException("Field config.label is emty!");
            }

            if (unitHost.getServiceFactory() == null) {
                throw new NotAvailableException("service factory");
            }

            this.config = config;
            this.unitHost = unitHost;
            this.serviceFactory = unitHost.getServiceFactory();
            this.location = new Location(unitHost.getLocationRegistry().getLocationConfigById(config.getPlacementConfig().getLocationId()));
            this.serviceList = new ArrayList<>();

            setField(TYPE_FILED_ID, getId());
            setField(TYPE_FILED_LABEL, getLabel());

            try {
                validateUpdateServices();
            } catch (MultiException ex) {
                logger.error(this + " is not valid!", ex);
                ex.printExceptionStack();
            }

            //TODO mpohling: is this still needed?
//            DALService.getRegistryProvider().getUnitRegistry().register(this);

            init(ScopeTransformer.transform(config.getScope()));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public final String getId() {
        return config.getId();
    }

    @Override
    public String getLabel() {
        return config.getLabel();
    }

    @Override
    public UnitTemplate.UnitType getType() {
        return config.getType();
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public UnitHost getUnitHost() {
        return unitHost;
    }

    @Override
    public UnitConfig getUnitConfig() {
        return config;
    }

    public Collection<Service> getServices() {
        return Collections.unmodifiableList(serviceList);
    }

    public void registerService(final Service service) {
        serviceList.add(service);
    }

    @Override
    public void registerMethods(RSBLocalServerInterface server) throws CouldNotPerformException {
        ServiceType.registerServiceMethods(server, this);
    }

    @Override
    public ServiceType getServiceType() {
        return ServiceType.MULTI;
    }

    @Override
    public ServiceFactory getServiceFactory() throws NotAvailableException {
        return serviceFactory;
    }

    /**
     * *
     *
     * @throws MultiException
     */
    private void validateUpdateServices() throws MultiException {
        logger.debug("Validating unit update methods...");

        MultiException.ExceptionStack exceptionStack = null;
        List<String> unitMethods = new ArrayList<>();

        // === Transform unit methods into string list. ===
        for (Method medhod : getClass().getMethods()) {
            unitMethods.add(medhod.getName());
        }

        // === Validate if all update methods are registrated. ===
        for (ServiceType service : ServiceType.getServiceTypeList(this)) {
            for (String serviceMethod : service.getUpdateMethods()) {
                if (!unitMethods.contains(serviceMethod)) {
                    exceptionStack = MultiException.push(service, null, exceptionStack);
                }
            }
        }

        // === throw multi exception in error case. ===
        MultiException.checkAndThrow("Update service not valid!", exceptionStack);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + config.getType() + "[" + config.getLabel() + "]]";
    }

    // TODO mpohing: please check if unit need to be a service!
    @Override
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
