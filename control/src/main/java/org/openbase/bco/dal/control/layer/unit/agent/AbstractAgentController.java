package org.openbase.bco.dal.control.layer.unit.agent;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.dal.control.layer.unit.AbstractExecutableBaseUnitController;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.unit.agent.AgentController;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.rst.processing.MetaConfigPool;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionInitiatorType.ActionInitiator;
import rst.domotic.action.ActionParameterType.ActionParameter;
import rst.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import rst.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActivationStateType.ActivationState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.agent.AgentClassType.AgentClass;
import rst.domotic.unit.agent.AgentDataType;
import rst.domotic.unit.agent.AgentDataType.AgentData;

import java.util.concurrent.ExecutionException;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractAgentController extends AbstractExecutableBaseUnitController<AgentData, AgentData.Builder> implements AgentController {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AgentData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ActivationState.getDefaultInstance()));
    }

    private ActionParameter defaultActionParameter;
    private String authenticationToken = null;

    public AbstractAgentController(final Class unitClass) throws InstantiationException {
        super(unitClass, AgentDataType.AgentData.newBuilder());
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        if(authenticationToken == null) {
            authenticationToken = requestAuthenticationToken(config);
        }

        // update default action parameter
        final AgentClass agentClass = Registries.getClassRegistry(true).getAgentClassById(config.getAgentConfig().getAgentClassId());
        defaultActionParameter = ActionParameter.newBuilder()
                .addAllCategory(agentClass.getCategoryList())
                .setPriority(agentClass.getPriority())
                .setSchedulable(agentClass.getSchedulable())
                .setInterruptible(agentClass.getInterruptible())
                .setAuthenticationToken(authenticationToken)
                .setActionInitiator(ActionInitiator.newBuilder().setInitiatorId(config.getId()))
                .build();
        return super.applyConfigUpdate(config);
    }

    private String requestAuthenticationToken(final UnitConfig agentUnitConfig) throws CouldNotPerformException, InterruptedException {
        try {
            UnitConfig agentUser = null;
            for (final UnitConfig userUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.USER)) {
                MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(userUnitConfig, userUnitConfig.getId()), userUnitConfig.getMetaConfig()));
                try {
                    String unitId = metaConfigPool.getValue(UnitUserCreationPlugin.UNIT_ID_KEY);
                    if (unitId.equalsIgnoreCase(agentUnitConfig.getId())) {
                        agentUser = userUnitConfig;
                        break;
                    }
                } catch (NotAvailableException ex) {
                    // do nothing
                }
            }

            if (agentUser == null) {
                throw new NotAvailableException("User for agent " + UnitConfigProcessor.getDefaultAlias(agentUnitConfig, agentUnitConfig.getId()));
            }

            final AuthenticationToken authenticationToken = AuthenticationToken.newBuilder().setUserId(agentUser.getId()).build();
            final SessionManager sessionManager = new SessionManager();
            sessionManager.login(agentUser.getId());
            final AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(authenticationToken, null, null);
            return new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get();
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not create authentication token for agent " + UnitConfigProcessor.getDefaultAlias(agentUnitConfig, agentUnitConfig.getId()), ex);
        }
    }

    private final static ProtoBufJSonProcessor protoBufJSonProcessor = new ProtoBufJSonProcessor();
    protected ActionDescription.Builder generateAction(final UnitType unitType, final ServiceType serviceType, final Message.Builder serviceArgumentBuilder) throws CouldNotPerformException {
        Message serviceArgument = serviceArgumentBuilder.build();
        try {
            return ActionDescriptionProcessor.generateActionDescriptionBuilder(getDefaultActionParameter().toBuilder()
                    .setServiceStateDescription(ServiceStateDescription.newBuilder()
                            .setServiceType(serviceType)
                            .setUnitType(unitType)
                            .setServiceAttributeType(protoBufJSonProcessor.getServiceAttributeType(serviceArgument))
                            .setServiceAttribute(protoBufJSonProcessor.serialize(serviceArgument))));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate action!", ex);
        }
    }


    protected ActionParameter getDefaultActionParameter() {
        return defaultActionParameter;
    }

    @Override
    protected boolean isAutostartEnabled() throws CouldNotPerformException {
        return getConfig().getAgentConfig().getAutostart();
    }
}
