package org.openbase.bco.device.openhab.manager.service;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.openbase.bco.dal.control.action.ActionImpl;
import org.openbase.bco.dal.control.layer.unit.AbstractUnitController;
import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.DiscoveryStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.device.openhab.manager.unit.OpenHABGatewayController;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.gateway.GatewayClassType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DiscoveryStateServiceImpl<ST extends DiscoveryStateOperationService & Unit<?>> extends OpenHABService<ST> implements DiscoveryStateOperationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryStateServiceImpl.class);

    private final String DISCOVERY_STATE_SERVICE_LOCK = "DISCOVERY_SERVICE_LOCK";

    public DiscoveryStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public ActivationState getDiscoveryState() throws NotAvailableException {
        return unit.getDiscoveryState();
    }

    @Override
    public Future<ActionDescription> setDiscoveryState(ActivationState discoveryState) {
        try {
            final AbstractUnitController<?, ?> unitController = (AbstractUnitController<?, ?>) unit;
            unitController.applyServiceState(discoveryState, ServiceTemplateType.ServiceTemplate.ServiceType.DISCOVERY_STATE_SERVICE);

            if (discoveryState.getValue() == ActivationState.State.ACTIVE) {
                // trigger discovery for binding at openHAB
                final GatewayClassType.GatewayClass gatewayClass = Registries.getClassRegistry().getGatewayClassById(unit.getConfig().getGatewayConfig().getGatewayClassId());
                final String bindingId = unit.generateVariablePool().getValue(OpenHABGatewayController.OPENHAB_BINDING_ID_META_CONFIG_KEY);
                final Integer discoveryTimeout = OpenHABRestCommunicator.getInstance().startDiscovery(bindingId);

                // apply returned timeout to the action
                final ActionImpl action = (ActionImpl) unitController.getActionById(discoveryState.getResponsibleAction().getActionId(), DISCOVERY_STATE_SERVICE_LOCK);
                action.setExecutionTimePeriod(discoveryTimeout, TimeUnit.SECONDS);
                unitController.reschedule();
            }

            return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(discoveryState, () -> ActionDescription.getDefaultInstance()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
