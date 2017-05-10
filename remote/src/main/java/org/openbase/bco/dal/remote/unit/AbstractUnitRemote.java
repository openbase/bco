package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.extension.protobuf.processing.GenericMessageProcessor;
import org.openbase.jul.extension.rsb.com.AbstractConfigurableRemote;
import org.openbase.jul.extension.rsb.com.RPCHelper;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rsb.scope.ScopeTransformer;
import org.openbase.jul.extension.rst.iface.ScopeProvider;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.slf4j.LoggerFactory;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <M>
 */
public abstract class AbstractUnitRemote<M extends GeneratedMessage> extends AbstractConfigurableRemote<M, UnitConfig> implements UnitRemote<M> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
    }

    private UnitTemplate template;
    private UnitRegistry unitRegistry;
    private final Map<ServiceType, MessageObservable> serviceStateObservableMap;

    public AbstractUnitRemote(final Class<M> dataClass) {
        super(dataClass, UnitConfig.class);

        serviceStateObservableMap = new HashMap<>();
    }

    protected UnitRegistry getUnitRegistry() throws InterruptedException, CouldNotPerformException {
        if (unitRegistry == null) {
            unitRegistry = Registries.getUnitRegistry();
            Registries.getUnitRegistry().waitForData();
        }
        return unitRegistry;
    }

    /**
     * {@inheritDoc}
     *
     * @param id
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void initById(final String id) throws InitializationException, InterruptedException {
        try {
            init(getUnitRegistry().getUnitConfigById(id));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param label
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void initByLabel(final String label) throws InitializationException, InterruptedException {
        try {
            List<UnitConfig> unitConfigList = getUnitRegistry().getUnitConfigsByLabel(label);

            if (unitConfigList.isEmpty()) {
                throw new NotAvailableException("Unit with Label[" + label + "]");
            } else if (unitConfigList.size() > 1) {
                throw new InvalidStateException("Unit with Label[" + label + "] is not unique!");
            }

            init(unitConfigList.get(0));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(ScopeType.Scope scope) throws InitializationException, InterruptedException {
        try {
            init(getUnitRegistry().getUnitConfigByScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(Scope scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeTransformer.transform(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param scope
     * @throws org.openbase.jul.exception.InitializationException
     * @throws java.lang.InterruptedException
     */
    @Override
    public void init(String scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeGenerator.generateScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void init(final String label, final ScopeProvider location) throws InitializationException, InterruptedException {
        try {
            init(ScopeGenerator.generateScope(label, getDataClass().getSimpleName(), location.getScope()));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws InitializationException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        this.setMessageProcessor(new GenericMessageProcessor<>(getDataClass()));
        ((UnitRegistryRemote) unitRegistry).addDataObserver((Observable<UnitRegistryData> source, UnitRegistryData data) -> {
            try {
                final UnitConfig newUnitConfig = unitRegistry.getUnitConfigById(getId());
                if (!newUnitConfig.equals(getConfig())) {
                    applyConfigUpdate(newUnitConfig);
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not update unit config of " + this, ex, logger);
            }
        });

        // TODO: move to applyConfigUpdate
        try {
            for (ServiceTemplate serviceTemplate : getTemplate().getServiceTemplateList()) {
                if (!serviceStateObservableMap.containsKey(serviceTemplate.getType())) {
                    serviceStateObservableMap.put(serviceTemplate.getType(), new MessageObservable(this));
                }
            }
        } catch (NotAvailableException ex) {

        }
        this.addDataObserver(new Observer<M>() {

            @Override
            public void update(Observable<M> source, M data) throws Exception {
                Set<ServiceType> serviceTypeSet = new HashSet<>();
                for (ServiceTemplate serviceTemplate : getTemplate().getServiceTemplateList()) {
                    if (!serviceTypeSet.contains(serviceTemplate.getType())) {
                        serviceTypeSet.add(serviceTemplate.getType());
                        try {
                            Object serviceData = Service.invokeProviderServiceMethod(serviceTemplate.getType(), data);
                            serviceStateObservableMap.get(serviceTemplate.getType()).notifyObservers(serviceData);
                        } catch (CouldNotPerformException ex) {
//                            System.out.println("update error, no service getter for type [" + serviceTemplate.getType() + "]");
                            logger.debug("Could not notify state update for service[" + serviceTemplate.getType() + "] because this service is not supported by this controller");
                        }
                    }
                }
            }
        });
    }

    @Override
    public void addServiceStateObserver(ServiceType serviceType, Observer observer) {
        serviceStateObservableMap.get(serviceType).addObserver(observer);
    }

    @Override
    public void removeServiceStateObserver(ServiceType serviceType, Observer observer) {
        serviceStateObservableMap.get(serviceType).removeObserver(observer);
    }

    /**
     * {@inheritDoc}
     *
     * @param config {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        if (config == null) {
            throw new NotAvailableException("UnitConfig");
        }

        try {
            if (getConfig().equals(config)) {
                logger.debug("Skip config update because no config change detected!");
                return config;
            }
        } catch (NotAvailableException ex) {
            // change check failed.
        }
        template = getUnitRegistry().getUnitTemplateByType(config.getType());
        return super.applyConfigUpdate(config);
    }

    /**
     * {@inheritDoc}
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        if (!isEnabled()) {
            throw new InvalidStateException("The activation of an remote is not allowed if the referred unit is disabled!");
        }
        if (!Units.contains(this)) {
            logger.warn("You are using a unit remote which is not maintained by the global unit remote pool! This is extremely inefficient! Please use \"Units.getUnit(...)\" instead creating your own instances!");
        }
        super.activate();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     */
    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        verifyEnablingState();
        super.waitForData();
    }

    /**
     * {@inheritDoc}
     *
     * @param timeout {@inheritDoc}
     * @param timeUnit {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public void waitForData(long timeout, TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        verifyEnablingState();
        super.waitForData(timeout, timeUnit);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        try {
            assert (getConfig() instanceof UnitConfig);
            return getConfig().getEnablingState().getValue().equals(EnablingState.State.ENABLED);
        } catch (CouldNotPerformException ex) {
            LoggerFactory.getLogger(org.openbase.bco.dal.lib.layer.unit.UnitRemote.class).warn("isEnabled() was called on non initialized unit!");
            assert false;
        }
        return false;
    }

    private void verifyEnablingState() throws FatalImplementationErrorException {
        if (!isEnabled()) {
            if (JPService.testMode()) {
                throw new FatalImplementationErrorException("Waiting for data of a disabled unit should be avoided!", "Calling instance");
            } else {
                logger.warn("Waiting for data of an disabled unit should be avoided! Probably this method will block forever!");
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public UnitType getType() throws NotAvailableException {
        try {
            return getConfig().getType();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit type", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public UnitTemplate getTemplate() throws NotAvailableException {
        if (template == null) {
            throw new NotAvailableException("UnitTemplate");
        }
        return template;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    public String getLabel() throws NotAvailableException {
        try {
            return getConfig().getLabel();
        } catch (NullPointerException | NotAvailableException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public ScopeType.Scope getScope() throws NotAvailableException {
        try {
            return getConfig().getScope();
        } catch (NullPointerException | CouldNotPerformException ex) {
            throw new NotAvailableException("unit label", ex);
        }
    }

    public UnitConfig getLocationConfig() throws NotAvailableException {
        try {
            // TODO implement get unit config by type and id;
            return unitRegistry.getUnitConfigById(getConfig().getPlacementConfig().getLocationId());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("LocationConfig", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param actionDescription {@inheritDoc}
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public Future<Void> applyAction(ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(actionDescription, this, Void.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws org.openbase.jul.exception.CouldNotPerformException {@inheritDoc}
     * @throws java.lang.InterruptedException {@inheritDoc}
     */
    @Override
    public Future<Snapshot> recordSnapshot() throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(this, Snapshot.class);
    }

    /**
     * Use if serviceType cannot be resolved from serviceAttribute. E.g. AlarmState.
     *
     * @param actionDescription
     * @param serviceAttribute
     * @param serviceType
     * @return
     * @throws CouldNotPerformException
     */
    protected ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Object serviceAttribute, final ServiceType serviceType) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        serviceStateDescription.setUnitId(getId());
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(getScope()));

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));
        //TODO: update USER key with authentification
        actionDescription.setLabel(actionDescription.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));

        return Service.upateActionDescription(actionDescription, serviceAttribute, serviceType);
    }

    /**
     * Default version.
     *
     * @param actionDescription
     * @param serviceAttribute
     * @return
     * @throws CouldNotPerformException
     */
    protected ActionDescription.Builder updateActionDescription(final ActionDescription.Builder actionDescription, final Object serviceAttribute) throws CouldNotPerformException {
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        ResourceAllocation.Builder resourceAllocation = actionDescription.getResourceAllocationBuilder();

        serviceStateDescription.setUnitId(getId());
        resourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(getScope()));

        actionDescription.setDescription(actionDescription.getDescription().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));
        //TODO: update USER key with authentification
        actionDescription.setLabel(actionDescription.getLabel().replace(ActionDescriptionProcessor.LABEL_KEY, getLabel()));

        return Service.upateActionDescription(actionDescription, serviceAttribute);
    }
}
