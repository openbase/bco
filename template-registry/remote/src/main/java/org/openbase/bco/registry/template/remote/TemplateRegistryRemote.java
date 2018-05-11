package org.openbase.bco.registry.template.remote;

/*
 * #%L
 * BCO Registry Template Remote
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

import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.bco.registry.template.lib.jp.JPTemplateRegistryScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.pattern.Remote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate;
import rst.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import rst.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class TemplateRegistryRemote extends AbstractRegistryRemote<TemplateRegistryData> implements TemplateRegistry, Remote<TemplateRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemplateRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ServiceTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, ActivityTemplate, ActivityTemplate.Builder> activityTemplateRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, ServiceTemplate, ServiceTemplate.Builder> serviceTemplateRemoteRegistry;
    private final SynchronizedRemoteRegistry<String, UnitTemplate, UnitTemplate.Builder> unitTemplateRemoteRegistry;

    public TemplateRegistryRemote() throws InstantiationException {
        super(JPTemplateRegistryScope.class, TemplateRegistryData.class);
        try {
            this.activityTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, TemplateRegistryData.ACTIVITY_TEMPLATE_FIELD_NUMBER);
            this.serviceTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, TemplateRegistryData.SERVICE_TEMPLATE_FIELD_NUMBER);
            this.unitTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getIntenalPriorizedDataObservable(), this, TemplateRegistryData.UNIT_TEMPLATE_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @throws InterruptedException     {@inheritDoc }
     * @throws CouldNotPerformException {@inheritDoc }
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (!CachedTemplateRegistryRemote.getRegistry().equals(this)) {
            logger.warn("You are using a " + getClass().getSimpleName() + " which is not maintained by the global registry singleton! This is extremely inefficient! Please use \"Registries.get" + getClass().getSimpleName().replace("Remote", "") + "()\" instead creating your own instances!");
        }
        super.activate();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() throws CouldNotPerformException {
        registerRemoteRegistry(activityTemplateRemoteRegistry);
        registerRemoteRegistry(serviceTemplateRemoteRegistry);
        registerRemoteRegistry(unitTemplateRemoteRegistry);
    }

    /**
     * Get the internal activity template remote registry.
     *
     * @return the internal activity template remote registry
     */
    public SynchronizedRemoteRegistry<String, ActivityTemplate, ActivityTemplate.Builder> getActivityTemplateRemoteRegistry() {
        return activityTemplateRemoteRegistry;
    }

    /**
     * Get the internal service template remote registry.
     *
     * @return the internal service template remote registry
     */
    public SynchronizedRemoteRegistry<String, ServiceTemplate, ServiceTemplate.Builder> getServiceTemplateRemoteRegistry() {
        return serviceTemplateRemoteRegistry;
    }

    /**
     * Get the internal unit template remote registry.
     *
     * @return the internal unit template remote registry
     */
    public SynchronizedRemoteRegistry<String, UnitTemplate, UnitTemplate.Builder> getUnitTemplateRemoteRegistry() {
        return unitTemplateRemoteRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}.
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Future<UnitTemplate> updateUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(unitTemplate, this, UnitTemplate.class);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.getMessage(unitTemplateId);
    }

    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException {
        validateData();
        return unitTemplateRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @param unitType {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public UnitTemplate getUnitTemplateByType(UnitType unitType) throws CouldNotPerformException {
        validateData();
        for (final UnitTemplate unitTemplate : unitTemplateRemoteRegistry.getMessages()) {
            if (unitTemplate.getType() == unitType) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("UnitTemplate with type [" + unitType + "]");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitTemplateRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        return getData().getUnitTemplateRegistryReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean isUnitTemplateRegistryConsistent() throws CouldNotPerformException {
        validateData();
        return getData().getUnitTemplateRegistryConsistent();
    }

    @Override
    public Future<ServiceTemplate> updateServiceTemplate(ServiceTemplate serviceTemplate) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(this, ServiceTemplate.class);
    }

    @Override
    public Boolean containsServiceTemplate(ServiceTemplate serviceTemplate) throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.contains(serviceTemplate);
    }

    @Override
    public Boolean containsServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.contains(serviceTemplateId);
    }

    @Override
    public ServiceTemplate getServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.getMessage(serviceTemplateId);
    }

    @Override
    public List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.getMessages();
    }

    @Override
    public ServiceTemplate getServiceTemplateByType(ServiceType serviceType) throws CouldNotPerformException {
        validateData();
        for (final ServiceTemplate serviceTemplate : serviceTemplateRemoteRegistry.getMessages()) {
            if (serviceTemplate.getType() == serviceType) {
                return serviceTemplate;
            }
        }
        throw new NotAvailableException("ServiceTemplate with type [" + serviceType + "]");
    }

    @Override
    public Boolean isServiceTemplateRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        return getData().getServiceTemplateRegistryReadOnly();
    }

    @Override
    public Boolean isServiceTemplateRegistryConsistent() throws CouldNotPerformException {
        validateData();
        return getData().getServiceTemplateRegistryConsistent();
    }

    @Override
    public Future<ActivityTemplate> updateActivityTemplate(ActivityTemplate activityTemplate) throws CouldNotPerformException {
        return RPCHelper.callRemoteMethod(this, ActivityTemplate.class);
    }

    @Override
    public Boolean containsActivityTemplate(ActivityTemplate activityTemplate) throws CouldNotPerformException {
        validateData();
        return activityTemplateRemoteRegistry.contains(activityTemplate);
    }

    @Override
    public Boolean containsActivityTemplateById(String activityTemplateId) throws CouldNotPerformException {
        validateData();
        return activityTemplateRemoteRegistry.contains(activityTemplateId);
    }

    @Override
    public ActivityTemplate getActivityTemplateById(String activityTemplateId) throws CouldNotPerformException {
        validateData();
        return activityTemplateRemoteRegistry.getMessage(activityTemplateId);
    }

    @Override
    public List<ActivityTemplate> getActivityTemplates() throws CouldNotPerformException {
        validateData();
        return activityTemplateRemoteRegistry.getMessages();
    }

    @Override
    public ActivityTemplate getActivityTemplateByType(ActivityType activityType) throws CouldNotPerformException {
        validateData();
        for (final ActivityTemplate activityTemplate : activityTemplateRemoteRegistry.getMessages()) {
            if (activityTemplate.getType() == activityType) {
                return activityTemplate;
            }
        }
        throw new NotAvailableException("ActivityTemplate with type [" + activityType + "]");
    }

    @Override
    public Boolean isActivityTemplateRegistryReadOnly() throws CouldNotPerformException {
        validateData();
        return getData().getActivityTemplateRegistryReadOnly();
    }

    @Override
    public Boolean isActivityTemplateRegistryConsistent() throws CouldNotPerformException {
        validateData();
        return getData().getServiceTemplateRegistryConsistent();
    }

    @Override
    public Boolean isConsistent() throws CouldNotPerformException {
        return isActivityTemplateRegistryConsistent()
                && isServiceTemplateRegistryConsistent()
                && isUnitTemplateRegistryConsistent();
    }
}
