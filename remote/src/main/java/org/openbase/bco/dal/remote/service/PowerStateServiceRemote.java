package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
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
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.MultiResourceAllocationStrategyType.MultiResourceAllocationStrategy;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateServiceRemote extends AbstractServiceRemote<PowerStateOperationService, PowerState> implements PowerStateOperationServiceCollection {

    public static final String META_CONFIG_UNIT_INFRASTRUCTURE_FLAG = "INFRASTRUCTURE";

    public static final boolean INFRASTRUCTURE_UNITS_FILTERED = true;
    public static final boolean INFRASTRUCTURE_UNITS_HANDELED = false;

    private boolean filterInfrastructureUnits;

    /**
     * Constructor creates a new service remote.
     *
     * Note: This remote instance totally ignores infrastructure units.
     */
    public PowerStateServiceRemote() {
        this(INFRASTRUCTURE_UNITS_FILTERED);
    }

    /**
     * Constructor creates a new service remote.
     *
     * Note: This remote instance totally ignores infrastructure units.
     *
     * @param filterInfrastructureUnits this flag defines if units which are marked as infrastructure are filtered by this instance.
     */
    public PowerStateServiceRemote(final boolean filterInfrastructureUnits) {
        super(ServiceType.POWER_STATE_SERVICE, PowerState.class);
        this.filterInfrastructureUnits = filterInfrastructureUnits;
    }

    @Override
    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
        try {
            // check if infrastructure filter is enabled
            if (filterInfrastructureUnits) {
                Registries.getUnitRegistry().waitForData();
                final MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider("UnitConfig", config.getMetaConfig()));
                if (config.hasUnitHostId() && !config.getUnitHostId().isEmpty()) {
                    metaConfigPool.register(new MetaConfigVariableProvider("UnitHost", Registries.getUnitRegistry().getUnitConfigById(config.getUnitHostId()).getMetaConfig()));
                }

                try {
                    //check if the unit is marked as infrastructure
                    if (Boolean.parseBoolean(metaConfigPool.getValue(META_CONFIG_UNIT_INFRASTRUCTURE_FLAG))) {
                        // do not handle infrastructure unit.
                        return;
                    }
                } catch (NotAvailableException ex) {
                    // META_CONFIG_UNIT_INFRASTRUCTURE_FLAG is not available so unit is not marked as infrastructure.
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
        // continue the init process
        super.init(config);
    }

    public Collection<PowerStateOperationService> getPowerStateOperationServices() {
        return getServices();
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState powerState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        actionDescription.setMultiResourceAllocationStrategy(MultiResourceAllocationStrategy.newBuilder().setStrategy(MultiResourceAllocationStrategy.Strategy.ALL_OR_NOTHING)).build();
        try {
            return applyAction(Services.updateActionDescription(actionDescription, powerState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set powerState", ex);
        }
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState powerState, final UnitType unitType) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitType(unitType);
        
        try {
            return applyAction(Services.updateActionDescription(actionDescription, powerState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set powerState", ex);
        }
    }

    /**
     * {@inheritDoc}
     * Computes the power state as on if at least one underlying service is on and else off.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected PowerState computeServiceState() throws CouldNotPerformException {
        return getPowerState(UnitType.UNKNOWN);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public PowerState getPowerState(final UnitType unitType) throws NotAvailableException {
        PowerState.State powerStateValue = PowerState.State.OFF;
        long timestamp = 0;
        for (PowerStateOperationService service : getServices(unitType)) {
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            if (service.getPowerState().getValue() == PowerState.State.ON) {
                powerStateValue = PowerState.State.ON;
            }

            timestamp = Math.max(timestamp, service.getPowerState().getTimestamp().getTime());
        }

        return TimestampProcessor.updateTimestamp(timestamp, PowerState.newBuilder().setValue(powerStateValue), TimeUnit.MICROSECONDS, logger).build();
    }

    /**
     * Must be called before init.
     *
     * @param enabled
     */
    @Override
    public void setInfrastructureFilter(boolean enabled) {
        this.filterInfrastructureUnits = enabled;
    }
}
