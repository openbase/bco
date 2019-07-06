package org.openbase.bco.registry.template.remote;

/*
 * #%L
 * BCO Registry Template Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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
import org.openbase.bco.registry.lib.com.RegistryVerifiedCommunicationHelper;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.bco.registry.template.lib.jp.JPTemplateRegistryScope;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.extension.type.util.TransactionSynchronizationFuture;
import org.openbase.jul.pattern.controller.Remote;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate.ActivityType;
import org.openbase.type.domotic.communication.TransactionValueType.TransactionValue;
import org.openbase.type.domotic.registry.TemplateRegistryDataType.TemplateRegistryData;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class TemplateRegistryRemote extends AbstractRegistryRemote<TemplateRegistryData> implements TemplateRegistry, Remote<TemplateRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TransactionValue.getDefaultInstance()));
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
            this.activityTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, TemplateRegistryData.ACTIVITY_TEMPLATE_FIELD_NUMBER);
            this.serviceTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, TemplateRegistryData.SERVICE_TEMPLATE_FIELD_NUMBER);
            this.unitTemplateRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, TemplateRegistryData.UNIT_TEMPLATE_FIELD_NUMBER);
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
     */
    @Override
    protected void registerRemoteRegistries() {
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
     */
    @Override
    public Future<UnitTemplate> updateUnitTemplate(UnitTemplate unitTemplate) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(unitTemplate, transactionValue -> updateUnitTemplateVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateUnitTemplateVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplate {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) {
        try {
            validateData();
            return unitTemplateRemoteRegistry.contains(unitTemplate);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param unitTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) {
        try {
        validateData();
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
        } catch (InvalidStateException e) {
            return true;
        }
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

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
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
            if (unitTemplate.getUnitType() == unitType) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("UnitTemplate with type [" + unitType + "]");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isUnitTemplateRegistryReadOnly() {
        try {
            validateData();
            return getData().getUnitTemplateRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isUnitTemplateRegistryConsistent() {
        try {
            validateData();
            return getData().getUnitTemplateRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTemplate {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<ServiceTemplate> updateServiceTemplate(ServiceTemplate serviceTemplate) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(serviceTemplate, transactionValue -> updateServiceTemplateVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateServiceTemplateVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTemplate {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsServiceTemplate(ServiceTemplate serviceTemplate) throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.contains(serviceTemplate);
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public Boolean containsServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.contains(serviceTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ServiceTemplate getServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.getMessage(serviceTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException {
        validateData();
        return serviceTemplateRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @param serviceType {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ServiceTemplate getServiceTemplateByType(ServiceType serviceType) throws CouldNotPerformException {
        validateData();
        for (final ServiceTemplate serviceTemplate : serviceTemplateRemoteRegistry.getMessages()) {
            if (serviceTemplate.getServiceType() == serviceType) {
                return serviceTemplate;
            }
        }
        throw new NotAvailableException("ServiceTemplate with type [" + serviceType + "]");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isServiceTemplateRegistryReadOnly() {
        try {
            validateData();
            return getData().getServiceTemplateRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isServiceTemplateRegistryConsistent() {
        try {
            validateData();
            return getData().getServiceTemplateRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param activityTemplate {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Future<ActivityTemplate> updateActivityTemplate(ActivityTemplate activityTemplate) {
        return RegistryVerifiedCommunicationHelper.requestVerifiedAction(activityTemplate, transactionValue -> updateActivityTemplateVerified(transactionValue));
    }

    @Override
    public Future<TransactionValue> updateActivityTemplateVerified(TransactionValue transactionValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(transactionValue, this, TransactionValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param activityTemplate {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsActivityTemplate(ActivityTemplate activityTemplate) {
        try {
            validateData();
            return activityTemplateRemoteRegistry.contains(activityTemplate);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param activityTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean containsActivityTemplateById(String activityTemplateId) {
        try {
            validateData();
            return activityTemplateRemoteRegistry.contains(activityTemplateId);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param activityTemplateId {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ActivityTemplate getActivityTemplateById(String activityTemplateId) throws CouldNotPerformException {
        validateData();
        return activityTemplateRemoteRegistry.getMessage(activityTemplateId);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<ActivityTemplate> getActivityTemplates() throws CouldNotPerformException {
        validateData();
        return activityTemplateRemoteRegistry.getMessages();
    }

    /**
     * {@inheritDoc}
     *
     * @param activityType {@inheritDoc}
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public ActivityTemplate getActivityTemplateByType(ActivityType activityType) throws CouldNotPerformException {
        validateData();
        for (final ActivityTemplate activityTemplate : activityTemplateRemoteRegistry.getMessages()) {
            if (activityTemplate.getActivityType() == activityType) {
                return activityTemplate;
            }
        }
        throw new NotAvailableException("ActivityTemplate with type [" + activityType + "]");
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isActivityTemplateRegistryReadOnly() {
        try {
            validateData();
            return getData().getActivityTemplateRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isActivityTemplateRegistryConsistent() {
        try {
            validateData();
            return getData().getServiceTemplateRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isConsistent() {
        return isActivityTemplateRegistryConsistent()
                && isServiceTemplateRegistryConsistent()
                && isUnitTemplateRegistryConsistent();
    }
}
