package org.openbase.bco.device.openhab.manager.service;

import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.operation.DiscoveryStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.device.openhab.communication.OpenHABRestCommunicator;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.state.ActivationStateType.ActivationState;
import org.openbase.type.domotic.unit.gateway.GatewayClassType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class DiscoveryStateServiceImpl<ST extends DiscoveryStateOperationService & Unit<?>> extends OpenHABService<ST> implements DiscoveryStateOperationService {

    public static final String OPENHAB_BINDING_ID_KEY = "OPENHAB_BINDING_ID";
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorStateServiceImpl.class);

    public DiscoveryStateServiceImpl(final ST unit) throws InstantiationException {
        super(unit);
    }

    @Override
    public ActivationState getDiscoveryState() throws NotAvailableException {
        return unit.getDiscoveryState();
    }

    @Override
    public Future<ActionDescription> setDiscoveryState(ActivationState discoveryState) {
        if (discoveryState.getValue() == ActivationState.State.INACTIVE) {
            return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(discoveryState, () -> ActionDescription.getDefaultInstance()));
        }

        try {
            final GatewayClassType.GatewayClass gatewayClass = Registries.getClassRegistry().getGatewayClassById(unit.getConfig().getGatewayConfig().getGatewayClassId());
            MetaConfigVariableProvider metaConfigVariableProvider = new MetaConfigVariableProvider("GatewayClassMetaConfig", gatewayClass.getMetaConfig());
            final String bindingId = metaConfigVariableProvider.getValue(OPENHAB_BINDING_ID_KEY);

            //TODO: apply this timeout to the action setting the state to active!
            final Integer discoveryTimeout = OpenHABRestCommunicator.getInstance().startDiscovery(bindingId);

            return FutureProcessor.completedFuture(ServiceStateProcessor.getResponsibleAction(discoveryState, () -> ActionDescription.getDefaultInstance()));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
