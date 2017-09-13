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
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.remote.unit.location.LocationRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
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
import org.openbase.jul.schedule.FutureProcessor;
import org.slf4j.LoggerFactory;
import rct.Transform;
import rsb.Scope;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.SnapshotType.Snapshot;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType.EnablingState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.rsb.ScopeType;
import org.openbase.bco.dal.lib.layer.service.Services;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <D> The unit data type.
 */
public abstract class AbstractUnitRemote<D extends GeneratedMessage> extends AbstractConfigurableRemote<D, UnitConfig> implements UnitRemote<D> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionFuture.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActionDescription.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Snapshot.getDefaultInstance()));
    }

    private UnitTemplate template;
    private boolean initialized = false;

    private final Observer<UnitRegistryData> unitRegistryObserver;
    private final Map<ServiceType, MessageObservable> serviceStateObservableMap;

    public AbstractUnitRemote(final Class<D> dataClass) {
        super(dataClass, UnitConfig.class);
        this.serviceStateObservableMap = new HashMap<>();
        this.unitRegistryObserver = new Observer<UnitRegistryData>() {
            @Override
            public void update(final Observable<UnitRegistryData> source, UnitRegistryData data) throws Exception {
                try {
                    final UnitConfig newUnitConfig = Registries.getUnitRegistry(true).getUnitConfigById(getId());
                    if (!newUnitConfig.equals(getConfig())) {
                        applyConfigUpdate(newUnitConfig);
                    }
                } catch (NotAvailableException ex) {
                    // unit config has been removed, probably because of deletion, Units will shutdown this remote
                    logger.debug("Could not update unit remote", ex);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update unit config of " + this, ex, logger);
                }
            }
        };
    }

    protected UnitRegistry getUnitRegistry() throws InterruptedException, CouldNotPerformException {
        Registries.getUnitRegistry().waitForData();
        return Registries.getUnitRegistry();
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
    public void init(final ScopeType.Scope scope) throws InitializationException, InterruptedException {
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
    public void init(final Scope scope) throws InitializationException, InterruptedException {
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
    public void init(final String scope) throws InitializationException, InterruptedException {
        try {
            this.init(ScopeGenerator.generateScope(scope));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Deprecated
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
        try {
            this.setMessageProcessor(new GenericMessageProcessor<>(getDataClass()));

            if (!initialized) {
                Registries.getUnitRegistry().addDataObserver(unitRegistryObserver);
                initialized = true;
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    protected void notifyDataUpdate(final D data) throws CouldNotPerformException {
        super.notifyDataUpdate(data);

        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final ServiceDescription serviceDescription : getUnitTemplate().getServiceDescriptionList()) {

            // check if already handled
            if (!serviceTypeSet.contains(serviceDescription.getType())) {
                serviceTypeSet.add(serviceDescription.getType());
                try {
                    Object serviceData = Services.invokeProviderServiceMethod(serviceDescription.getType(), data);
                    serviceStateObservableMap.get(serviceDescription.getType()).notifyObservers(serviceData);
                } catch (CouldNotPerformException ex) {
                    logger.debug("Could not notify state update for service[" + serviceDescription.getType() + "] because this service is not supported by this remote controller.", ex);
                }
            }
        }
    }

    @Override
    public void addServiceStateObserver(final ServiceType serviceType, final Observer observer) {
        serviceStateObservableMap.get(serviceType).addObserver(observer);
    }

    @Override
    public void removeServiceStateObserver(final ServiceType serviceType, final Observer observer) {
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

        // non change filter
        try {
            if (getConfig().equals(config)) {
                logger.debug("Skip config update because no config change detected!");
                return config;
            }
        } catch (final NotAvailableException ex) {
            logger.trace("Unit config change check failed because config is not available yet.");
        }

        // update unit template
        template = getUnitRegistry().getUnitTemplateByType(config.getType());

        // register service observable which are not handled yet.
        for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

            // create observable if new
            if (!serviceStateObservableMap.containsKey(serviceDescription.getType())) {
                serviceStateObservableMap.put(serviceDescription.getType(), new MessageObservable(this));
            }
        }

        // cleanup service observable related to new unit template
        outer:
        for (final ServiceType serviceType : serviceStateObservableMap.keySet()) {
            for (final ServiceDescription serviceDescription : template.getServiceDescriptionList()) {

                // verify if service type is still valid.
                if (serviceType == serviceDescription.getType()) {
                    // continue because service type is still valid
                    continue outer;
                }
            }

            // remove and shutdown service observable because its not valid
            serviceStateObservableMap.remove(serviceType).shutdown();
        }

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
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
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
    public UnitType getUnitType() throws NotAvailableException {
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
    public UnitTemplate getUnitTemplate() throws NotAvailableException {
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

    /**
     * Method returns the parent location config of this unit.
     *
     * @return a unit config of the parent location.
     * @throws NotAvailableException is thrown if the location config is currently not available.
     */
    public UnitConfig getParentLocationConfig() throws NotAvailableException, InterruptedException {
        try {
            // TODO implement get unit config by type and id;
            return getUnitRegistry().getUnitConfigById(getConfig().getPlacementConfig().getLocationId());
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("LocationConfig", ex);
        }
    }

    /**
     *
     * @return
     * @throws NotAvailableException
     * @deprecated please use getParentLocationConfig() instead.
     */
    @Deprecated
    public UnitConfig getLocationConfig() throws NotAvailableException {
        try {
            return getParentLocationConfig();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new NotAvailableException(ex);
        }
    }

    /**
     * Method returns the parent location remote of this unit.
     *
     * @param waitForData flag defines if the method should block until the remote is fully synchronized.
     * @return a location remote instance.
     * @throws NotAvailableException is thrown if the location remote is currently not available.
     * @throws java.lang.InterruptedException is thrown if the current was externally interrupted.
     */
    public LocationRemote getParentLocationRemote(final boolean waitForData) throws NotAvailableException, InterruptedException {
        try {
            return Units.getUnit(getConfig().getPlacementConfig().getLocationId(), waitForData, Units.LOCATION);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("LocationRemote", ex);
        }
    }

    /**
     * Method returns the transformation between the root location and this unit.
     *
     * @return a transformation future
     * @throws InterruptedException is thrown if the thread was externally interrupted.
     * @deprecated please use {@code getRootToUnitTransformationFuture()} instead.
     */
    @Deprecated
    public Future<Transform> getTransformation() throws InterruptedException {
        try {
            return Units.getRootToUnitTransformationFuture(getConfig());
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(Transform.class, new NotAvailableException("UnitTransformation", ex));
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
    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException, InterruptedException {
        return RPCHelper.callRemoteMethod(actionDescription, this, ActionFuture.class);
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

        return Services.upateActionDescription(actionDescription, serviceAttribute, serviceType);
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

        return Services.upateActionDescription(actionDescription, serviceAttribute);
    }

    @Override
    public void shutdown() {
        super.shutdown();

        try {
            // shutdown registry observer
            Registries.getUnitRegistry().removeDataObserver(unitRegistryObserver);
        } catch (final NotAvailableException ex) {
            // if the registry is not any longer available (in case of registry shutdown) than the observer is already cleared
        } catch (final Exception ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove unit registry observer.", ex), logger);
        }

        // shutdown service observer
        for (final MessageObservable serviceObservable : serviceStateObservableMap.values()) {
            serviceObservable.shutdown();
        }
    }
}
