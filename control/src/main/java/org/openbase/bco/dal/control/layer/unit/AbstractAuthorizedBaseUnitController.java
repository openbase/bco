package org.openbase.bco.dal.control.layer.unit;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Message;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.core.plugin.UnitUserCreationPlugin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.action.ActionParameterType.ActionParameter;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.AuthenticationTokenType.AuthenticationToken;
import org.openbase.type.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;

public abstract class AbstractAuthorizedBaseUnitController<D extends AbstractMessage & Serializable, DB extends D.Builder<DB>> extends AbstractExecutableBaseUnitController<D, DB> {

    private ActionParameter defaultActionParameter;
    private String authenticationToken = null;

    private final static ProtoBufJSonProcessor protoBufJSonProcessor = new ProtoBufJSonProcessor();

    public AbstractAuthorizedBaseUnitController(Class unitClass, DB builder) throws InstantiationException {
        super(unitClass, builder);
    }

    @Override
    public UnitConfig applyConfigUpdate(final UnitConfig config) throws CouldNotPerformException, InterruptedException {
        if (authenticationToken == null) {
            authenticationToken = requestAuthenticationToken(config);
        }

        // update default action parameter
        defaultActionParameter = getActionParameterTemplate(config)
                .setAuthenticationToken(authenticationToken)
                .setActionInitiator(ActionInitiator.newBuilder().setInitiatorId(config.getId()))
                .build();
        return super.applyConfigUpdate(config);
    }

    protected abstract ActionParameter.Builder getActionParameterTemplate(final UnitConfig config) throws InterruptedException, CouldNotPerformException;

    private String requestAuthenticationToken(final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        try {
            UnitConfig userUnitConfig = null;
            for (final UnitConfig config : Registries.getUnitRegistry().getUnitConfigs(UnitType.USER)) {
                MetaConfigPool metaConfigPool = new MetaConfigPool();
                metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(config, config.getId()), config.getMetaConfig()));
                try {
                    String unitId = metaConfigPool.getValue(UnitUserCreationPlugin.UNIT_ID_KEY);
                    if (unitId.equalsIgnoreCase(unitConfig.getId())) {
                        userUnitConfig = config;
                        break;
                    }
                } catch (NotAvailableException ex) {
                    // do nothing
                }
            }

            if (userUnitConfig == null) {
                throw new NotAvailableException("User for "+this+" " + UnitConfigProcessor.getDefaultAlias(unitConfig, unitConfig.getId()));
            }

            final AuthenticationToken authenticationToken = AuthenticationToken.newBuilder().setUserId(userUnitConfig.getId()).build();
            final SessionManager sessionManager = new SessionManager();
            sessionManager.login(userUnitConfig.getId());
            final AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(authenticationToken, null, null);
            return new AuthenticatedValueFuture<>(
                    Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                    String.class,
                    authenticatedValue.getTicketAuthenticatorWrapper(),
                    sessionManager).get();
        } catch (CouldNotPerformException | ExecutionException ex) {
            throw new CouldNotPerformException("Could not create authentication token for "+this+" " + UnitConfigProcessor.getDefaultAlias(unitConfig, unitConfig.getId()), ex);
        }
    }

    protected ActionParameter.Builder generateAction(final UnitType unitType, final ServiceType serviceType, final Message.Builder serviceArgumentBuilder) throws CouldNotPerformException {
        final Message serviceArgument = serviceArgumentBuilder.build();
        try {
            return defaultActionParameter.toBuilder()
                    .setServiceStateDescription(ServiceStateDescription.newBuilder()
                            .setServiceType(serviceType)
                            .setUnitType(unitType)
                            .setServiceAttributeType(protoBufJSonProcessor.getServiceAttributeType(serviceArgument))
                            .setServiceAttribute(protoBufJSonProcessor.serialize(serviceArgument)));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate action!", ex);
        }
    }

    protected ActionParameter getDefaultActionParameter() {
        return defaultActionParameter;
    }
}
