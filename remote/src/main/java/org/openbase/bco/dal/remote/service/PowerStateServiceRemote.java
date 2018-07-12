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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.PowerStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.state.PowerStateType.PowerState.State;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Collection;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class PowerStateServiceRemote extends AbstractServiceRemote<PowerStateOperationService, PowerState> implements PowerStateOperationServiceCollection {

//    public static final String META_CONFIG_UNIT_INFRASTRUCTURE_FLAG = "INFRASTRUCTURE";
//
//    public static final boolean INFRASTRUCTURE_UNITS_FILTERED = true;
//    public static final boolean INFRASTRUCTURE_UNITS_HANDELED = false;
//
//    private boolean filterInfrastructureUnits;

    /**
     * Constructor creates a new service remote.
     * <p>
     * Note: This remote instance totally ignores infrastructure units.
     */
    public PowerStateServiceRemote() {
        this(true);
    }

    /**
     * Constructor creates a new service remote.
     * <p>
     * Note: This remote instance totally ignores infrastructure units.
     *
     * @param filterInfrastructureUnits this flag defines if units which are marked as infrastructure are filtered by this instance.
     */
    public PowerStateServiceRemote(final boolean filterInfrastructureUnits) {
        super(ServiceType.POWER_STATE_SERVICE, PowerState.class, filterInfrastructureUnits);
//        this.filterInfrastructureUnits = filterInfrastructureUnits;
    }

//    @Override
//    public void init(final UnitConfig config) throws InitializationException, InterruptedException {
//        try {
//            // check if infrastructure filter is enabled
//            if (filterInfrastructureUnits) {
//                Registries.waitForData();
//                final MetaConfigPool metaConfigPool = new MetaConfigPool();
//                metaConfigPool.register(new MetaConfigVariableProvider("UnitConfig", config.getMetaConfig()));
//                if (config.hasUnitHostId() && !config.getUnitHostId().isEmpty()) {
//                    try {
//                        metaConfigPool.register(new MetaConfigVariableProvider("UnitHost", Registries.getUnitRegistry().getUnitConfigById(config.getUnitHostId()).getMetaConfig()));
//                    } catch (NotAvailableException ex) {
//                        logger.warn("Could not check host of unit[" + ScopeGenerator.generateStringRep(config.getScope()) + "] for infrastructure filter because its not available");
//                    }
//                }
//
//                try {
//                    //check if the unit is marked as infrastructure
//                    if (Boolean.parseBoolean(metaConfigPool.getValue(META_CONFIG_UNIT_INFRASTRUCTURE_FLAG))) {
//                        // do not handle infrastructure unit.
//                        return;
//                    }
//                } catch (NotAvailableException ex) {
//                    // META_CONFIG_UNIT_INFRASTRUCTURE_FLAG is not available so unit is not marked as infrastructure.
//                }
//            }
//        } catch (CouldNotPerformException ex) {
//            throw new InitializationException(this, ex);
//        }
//        // continue the init process
//        super.init(config);
//    }

    public Collection<PowerStateOperationService> getPowerStateOperationServices() {
        return getServices();
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState powerState) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(powerState, getServiceType()));
    }

    @Override
    public Future<ActionFuture> setPowerState(final PowerState powerState, final UnitType unitType) throws CouldNotPerformException {
        return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(powerState, getServiceType(), unitType));
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
        return getData();
    }

    @Override
    public PowerState getPowerState(final UnitType unitType) throws NotAvailableException {
        try {
            return (PowerState) generateFusedState(unitType, State.OFF, State.ON).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException(Services.getServiceStateName(getServiceType()), ex);
        }
    }
}
