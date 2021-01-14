package org.openbase.bco.registry.template.core;

/*
 * #%L
 * BCO Registry Template Core
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

import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.com.RegistryVerifiedCommunicationHelper;
import org.openbase.bco.registry.lib.generator.UUIDGenerator;
import org.openbase.bco.registry.template.core.consistency.activitytemplate.ActivityTemplateUniqueTypeConsistencyHandler;
import org.openbase.bco.registry.template.core.consistency.servicetemplate.ServiceTemplateUniqueTypeConsistencyHandler;
import org.openbase.bco.registry.template.core.consistency.unittemplate.UnitTemplateUniqueTypeConsistencyHandler;
import org.openbase.bco.registry.template.core.consistency.unittemplate.UniteTemplateServiceTemplateConsistencyHandler;
import org.openbase.bco.registry.template.core.plugin.ActivityTemplateCreatorRegistryPlugin;
import org.openbase.bco.registry.template.core.plugin.ServiceTemplateCreatorRegistryPlugin;
import org.openbase.bco.registry.template.core.plugin.UnitTemplateCreatorRegistryPlugin;
import org.openbase.bco.registry.template.lib.TemplateRegistry;
import org.openbase.bco.registry.template.lib.jp.JPActivityTemplateDatabaseDirectory;
import org.openbase.bco.registry.template.lib.jp.JPServiceTemplateDatabaseDirectory;
import org.openbase.bco.registry.template.lib.jp.JPTemplateRegistryScope;
import org.openbase.bco.registry.template.lib.jp.JPUnitTemplateDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.iface.Manageable;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
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
import org.openbase.type.communication.ScopeType;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class TemplateRegistryController extends AbstractRegistryController<TemplateRegistryData, TemplateRegistryData.Builder> implements TemplateRegistry, Manageable<ScopeType.Scope> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TransactionValue.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemplateRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivityTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UnitTemplate.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ServiceTemplate.getDefaultInstance()));
    }

    private final ProtoBufFileSynchronizedRegistry<String, ActivityTemplate, ActivityTemplate.Builder, TemplateRegistryData.Builder> activityTemplateRemoteRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, ServiceTemplate, ServiceTemplate.Builder, TemplateRegistryData.Builder> serviceTemplateRemoteRegistry;
    private final ProtoBufFileSynchronizedRegistry<String, UnitTemplate, UnitTemplate.Builder, TemplateRegistryData.Builder> unitTemplateRemoteRegistry;

    public TemplateRegistryController() throws InstantiationException, InterruptedException {
        super(JPTemplateRegistryScope.class, TemplateRegistryData.newBuilder());
        try {
            activityTemplateRemoteRegistry = new ProtoBufFileSynchronizedRegistry<>(ActivityTemplate.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(TemplateRegistryData.ACTIVITY_TEMPLATE_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPActivityTemplateDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);

            serviceTemplateRemoteRegistry = new ProtoBufFileSynchronizedRegistry<>(ServiceTemplate.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(TemplateRegistryData.SERVICE_TEMPLATE_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPServiceTemplateDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);

            unitTemplateRemoteRegistry = new ProtoBufFileSynchronizedRegistry<>(UnitTemplate.class,
                    getBuilderSetup(),
                    getDataFieldDescriptor(TemplateRegistryData.UNIT_TEMPLATE_FIELD_NUMBER),
                    new UUIDGenerator<>(),
                    JPService.getProperty(JPUnitTemplateDatabaseDirectory.class).getValue(),
                    protoBufJSonFileProvider,
                    false);
        } catch (JPServiceException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerRegistries() throws CouldNotPerformException {
        registerRegistry(activityTemplateRemoteRegistry);
        registerRegistry(serviceTemplateRemoteRegistry);
        registerRegistry(unitTemplateRemoteRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
        activityTemplateRemoteRegistry.registerConsistencyHandler(new ActivityTemplateUniqueTypeConsistencyHandler());

        serviceTemplateRemoteRegistry.registerConsistencyHandler(new ServiceTemplateUniqueTypeConsistencyHandler());

        unitTemplateRemoteRegistry.registerConsistencyHandler(new UnitTemplateUniqueTypeConsistencyHandler());
        unitTemplateRemoteRegistry.registerConsistencyHandler(new UniteTemplateServiceTemplateConsistencyHandler(serviceTemplateRemoteRegistry));
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
        activityTemplateRemoteRegistry.registerPlugin(new ActivityTemplateCreatorRegistryPlugin(activityTemplateRemoteRegistry));
        serviceTemplateRemoteRegistry.registerPlugin(new ServiceTemplateCreatorRegistryPlugin(serviceTemplateRemoteRegistry));
        unitTemplateRemoteRegistry.registerPlugin(new UnitTemplateCreatorRegistryPlugin(unitTemplateRemoteRegistry));
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        unitTemplateRemoteRegistry.registerDependency(serviceTemplateRemoteRegistry);
    }

    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException {
        setDataField(TemplateRegistryData.ACTIVITY_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, activityTemplateRemoteRegistry.isReadOnly());
        setDataField(TemplateRegistryData.ACTIVITY_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, activityTemplateRemoteRegistry.isConsistent());

        setDataField(TemplateRegistryData.SERVICE_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, serviceTemplateRemoteRegistry.isReadOnly());
        setDataField(TemplateRegistryData.SERVICE_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, serviceTemplateRemoteRegistry.isConsistent());

        setDataField(TemplateRegistryData.UNIT_TEMPLATE_REGISTRY_READ_ONLY_FIELD_NUMBER, unitTemplateRemoteRegistry.isReadOnly());
        setDataField(TemplateRegistryData.UNIT_TEMPLATE_REGISTRY_CONSISTENT_FIELD_NUMBER, unitTemplateRemoteRegistry.isConsistent());
    }

    @Override
    protected void registerRemoteRegistries() {
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(TemplateRegistry.class, this, server);
    }

    @Override
    public Future<UnitTemplate> updateUnitTemplate(final UnitTemplate unitTemplate) {
        return GlobalCachedExecutorService.submit(() -> unitTemplateRemoteRegistry.update(unitTemplate));
    }

    @Override
    public Future<TransactionValue> updateUnitTemplateVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, UnitTemplate.class, this::updateUnitTemplate);
    }

    @Override
    public Boolean containsUnitTemplate(UnitTemplate unitTemplate) {
        return unitTemplateRemoteRegistry.contains(unitTemplate);
    }

    @Override
    public Boolean containsUnitTemplateById(String unitTemplateId) {
        return unitTemplateRemoteRegistry.contains(unitTemplateId);
    }

    @Override
    public UnitTemplate getUnitTemplateById(String unitTemplateId) throws CouldNotPerformException {
        return unitTemplateRemoteRegistry.getMessage(unitTemplateId);
    }

    @Override
    public List<UnitTemplate> getUnitTemplates() throws CouldNotPerformException {
        return unitTemplateRemoteRegistry.getMessages();
    }

    @Override
    public UnitTemplate getUnitTemplateByType(UnitType unitType) throws CouldNotPerformException {
        for (final UnitTemplate unitTemplate : unitTemplateRemoteRegistry.getMessages()) {
            if (unitTemplate.getUnitType() == unitType) {
                return unitTemplate;
            }
        }
        throw new NotAvailableException("UnitTemplate with type [" + unitType + "]");
    }

    @Override
    public Boolean isUnitTemplateRegistryReadOnly() {
        return unitTemplateRemoteRegistry.isReadOnly();
    }

    @Override
    public Boolean isUnitTemplateRegistryConsistent() {
        return unitTemplateRemoteRegistry.isConsistent();
    }

    @Override
    public Future<ServiceTemplate> updateServiceTemplate(ServiceTemplate serviceTemplate) {
        return GlobalCachedExecutorService.submit(() -> serviceTemplateRemoteRegistry.update(serviceTemplate));
    }

    @Override
    public Future<TransactionValue> updateServiceTemplateVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, ServiceTemplate.class, this::updateServiceTemplate);
    }

    @Override
    public Boolean containsServiceTemplate(ServiceTemplate serviceTemplate) {
        return serviceTemplateRemoteRegistry.contains(serviceTemplate);
    }

    @Override
    public Boolean containsServiceTemplateById(String serviceTemplateId) {
        return serviceTemplateRemoteRegistry.contains(serviceTemplateId);
    }

    @Override
    public ServiceTemplate getServiceTemplateById(String serviceTemplateId) throws CouldNotPerformException {
        return serviceTemplateRemoteRegistry.getMessage(serviceTemplateId);
    }

    @Override
    public List<ServiceTemplate> getServiceTemplates() throws CouldNotPerformException {
        return serviceTemplateRemoteRegistry.getMessages();
    }

    @Override
    public ServiceTemplate getServiceTemplateByType(ServiceType serviceType) throws CouldNotPerformException {
        for (final ServiceTemplate serviceTemplate : serviceTemplateRemoteRegistry.getMessages()) {
            if (serviceTemplate.getServiceType() == serviceType) {
                return serviceTemplate;
            }
        }
        throw new NotAvailableException("ServiceTemplate with type [" + serviceType + "]");
    }

    @Override
    public Boolean isServiceTemplateRegistryReadOnly() {
        return serviceTemplateRemoteRegistry.isReadOnly();
    }

    @Override
    public Boolean isServiceTemplateRegistryConsistent() {
        return serviceTemplateRemoteRegistry.isConsistent();
    }

    @Override
    public Future<ActivityTemplate> updateActivityTemplate(ActivityTemplate activityTemplate) {
        return GlobalCachedExecutorService.submit(() -> activityTemplateRemoteRegistry.update(activityTemplate));
    }

    @Override
    public Future<TransactionValue> updateActivityTemplateVerified(TransactionValue transactionValue) {
        return RegistryVerifiedCommunicationHelper.executeVerifiedAction(transactionValue, this, ActivityTemplate.class, this::updateActivityTemplate);
    }

    @Override
    public Boolean containsActivityTemplate(ActivityTemplate activityTemplate) {
        return activityTemplateRemoteRegistry.contains(activityTemplate);
    }

    @Override
    public Boolean containsActivityTemplateById(String activityTemplateId) {
        return activityTemplateRemoteRegistry.contains(activityTemplateId);
    }

    @Override
    public ActivityTemplate getActivityTemplateById(String activityTemplateId) throws CouldNotPerformException {
        return activityTemplateRemoteRegistry.getMessage(activityTemplateId);
    }

    @Override
    public List<ActivityTemplate> getActivityTemplates() throws CouldNotPerformException {
        return activityTemplateRemoteRegistry.getMessages();
    }

    @Override
    public ActivityTemplate getActivityTemplateByType(ActivityType activityType) throws CouldNotPerformException {
        for (final ActivityTemplate activityTemplate : activityTemplateRemoteRegistry.getMessages()) {
            if (activityTemplate.getActivityType() == activityType) {
                return activityTemplate;
            }
        }
        throw new NotAvailableException("ActivityTemplate with type [" + activityType + "]");
    }

    @Override
    public Boolean isActivityTemplateRegistryReadOnly() {
        return activityTemplateRemoteRegistry.isReadOnly();
    }

    @Override
    public Boolean isActivityTemplateRegistryConsistent() {
        return activityTemplateRemoteRegistry.isConsistent();
    }
}
