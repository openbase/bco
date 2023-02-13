package org.openbase.bco.api.graphql.schema;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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

import com.google.api.graphql.rejoiner.Arg;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.collect.ImmutableList;
import graphql.schema.DataFetchingEnvironment;
import org.openbase.bco.api.graphql.context.AbstractBCOGraphQLContext;
import org.openbase.bco.api.graphql.error.ArgumentError;
import org.openbase.bco.api.graphql.error.BCOGraphQLError;
import org.openbase.bco.api.graphql.error.GenericError;
import org.openbase.bco.api.graphql.error.ServerError;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture;
import org.openbase.bco.authentication.lib.iface.BCOSession;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.session.BCOSessionImpl;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.type.configuration.EntryType;
import org.openbase.type.configuration.MetaConfigType;
import org.openbase.type.domotic.authentication.AuthTokenType.AuthToken;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter;
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.geometry.PoseType;
import org.openbase.type.language.LabelType;
import org.openbase.type.spatial.PlacementConfigType;
import org.openbase.type.spatial.ShapeType;

import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class RegistrySchemaModule extends SchemaModule {

    // ===================================== Queries ===================================================================

    public static ImmutableList<UnitConfig> getUnitConfigs(final UnitFilter unitFilter, Boolean includeDisabledUnits) throws BCOGraphQLError {
        try {
            if ((includeDisabledUnits == null)) {
                includeDisabledUnits = false;
            }
            return ImmutableList.copyOf(
                    Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigs(includeDisabledUnits, unitFilter));

        } catch (RuntimeException | CouldNotPerformException | InterruptedException ex) {
            throw new GenericError(ex);
        }
    }

    /**
     * Check if an authentication token retrieved by the login method is still valid.
     *
     * @param token the token to be checked
     *
     * @return if the token is valid and can be used to authenticate further requests
     */
    @Query("verifyToken")
    Boolean verifyToken(@Arg("token") String token) {
        //TODO: blocked by https://github.com/openbase/bco.registry/issues/108
        return true;
    }

    /**
     * Method can be used to request a token of the given bco user.
     *
     * @param username     the name of the user as plain text string.
     * @param passwordHash the password hash of the user that need to be generted first (see note below).
     *
     * @return the login token.
     * <p>
     * Note: The hash of the default admin password is: '''R+gZ+PFuauhav8rRVa3XlWXXSEyi5BcdrbeXLEY3tDQ='''
     *
     * @throws BCOGraphQLError
     */
    @Query("login")
    String login(@Arg("username") String username, @Arg("password") String passwordHash) throws BCOGraphQLError {
        try {
            final BCOSession session = new BCOSessionImpl();

            session.loginUserViaUsername(username, Base64.getDecoder().decode(passwordHash), false);
            return session.generateAuthToken(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getAuthenticationToken();
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Query("changePassword")
    Boolean changePassword(@Arg("username") String username, @Arg("oldPassword") String oldPassword, @Arg("newPassword") String newPassword) throws BCOGraphQLError {
        try {
            final String userId = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUserUnitIdByUserName(username);

            final SessionManager sessionManager = new SessionManager();
            try {
                sessionManager.loginUser(userId, oldPassword, false);
            } catch (CouldNotPerformException ex) {
                throw new ArgumentError(ex);
            }
            sessionManager.changePassword(userId, oldPassword, newPassword).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);

            return true;
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Query("unitConfig")
    UnitConfig getUnitConfigById(@Arg("id") String id, DataFetchingEnvironment env) throws BCOGraphQLError {
        try {
            return Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigById(id);
        } catch (RuntimeException | CouldNotPerformException | InterruptedException ex) {
            throw new GenericError(ex);
        }
    }

    @Query("unitConfigs")
    ImmutableList<UnitConfig> queryGetUnitConfigs(@Arg("filter") UnitFilter unitFilter, @Arg("includeDisabledUnits") Boolean includeDisabledUnits) throws BCOGraphQLError {
        return getUnitConfigs(unitFilter, includeDisabledUnits);
    }

    // ===================================== Mutations =================================================================

    @Query("gatewayClasses")
    ImmutableList<GatewayClass> gatewayClasses() throws CouldNotPerformException, InterruptedException {
        return ImmutableList.copyOf(Registries.getClassRegistry(true).getGatewayClasses());
    }

    @Mutation("updateUnitConfig")
    UnitConfig updateUnitConfig(@Arg("unitConfig") UnitConfig unitConfig) throws BCOGraphQLError {
        try {
            final UnitConfig.Builder unitConfigBuilder = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT)
                    .getUnitConfigById(unitConfig.getId())
                    .toBuilder();
            ProtoBufBuilderProcessor.mergeFromWithoutRepeatedFields(unitConfigBuilder, unitConfig);
            return Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).updateUnitConfig(unitConfigBuilder.build()).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Mutation("removeUnitConfig")
    UnitConfig removeUnitConfig(@Arg("unitId") String unitId) throws BCOGraphQLError {
        try {
            final UnitConfig unitConfig = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigById(unitId);
            return Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).removeUnitConfig(unitConfig).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Mutation("registerUnitConfig")
    UnitConfig registerUnitConfig(@Arg("unitConfig") UnitConfig unitConfig) throws BCOGraphQLError {
        try {
            return Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).registerUnitConfig(unitConfig).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Mutation("updateLabel")
    LabelType.Label updateLabel(@Arg("unitId") String unitId, @Arg("label") String label, DataFetchingEnvironment env) throws BCOGraphQLError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigById(unitId).toBuilder();

            final AbstractBCOGraphQLContext context = env.getContext();
            final String oldLabel = LabelProcessor.getBestMatch(context.getLanguageCode(), builder.getLabel());
            LabelProcessor.replace(builder.getLabelBuilder(), oldLabel, label);

            return Registries.getUnitRegistry().updateUnitConfig(builder.build()).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getLabel();
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Mutation("updateLocation")
    PlacementConfigType.PlacementConfig updateLocation(@Arg("unitId") String unitId, @Arg("locationId") String locationId) throws BCOGraphQLError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigById(unitId).toBuilder();
            builder.getPlacementConfigBuilder().setLocationId(locationId);
            return Registries.getUnitRegistry().updateUnitConfig(builder.build()).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getPlacementConfig();
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Mutation("updateFloorPlan")
    ShapeType.Shape updateFloorPlan(@Arg("locationId") String locationId, @Arg("shape") ShapeType.Shape shape) throws BCOGraphQLError {
        try {
            final UnitConfig.Builder unitConfigBuilder = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigById(locationId).toBuilder();
            unitConfigBuilder.getPlacementConfigBuilder().getShapeBuilder().clearFloor().addAllFloor(shape.getFloorList());
            return Registries.getUnitRegistry().updateUnitConfig(unitConfigBuilder.build()).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getPlacementConfig().getShape();
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    @Mutation("updatePose")
    PoseType.Pose updatePose(@Arg("unitId") String unitId, @Arg("pose") PoseType.Pose pose) throws BCOGraphQLError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getUnitConfigById(unitId).toBuilder();
            builder.getPlacementConfigBuilder().clearPose().setPose(pose);
            return Registries.getUnitRegistry().updateUnitConfig(builder.build()).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT).getPlacementConfig().getPose();
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException |
                 TimeoutException ex) {
            throw new GenericError(ex);
        }
    }

    // ===================================== Service Methods ===========================================================

    @Mutation("updateMetaConfig")
    MetaConfigType.MetaConfig updateMetaConfig(@Arg("unitId") String unitId, @Arg("entry") EntryType.Entry entry, DataFetchingEnvironment env) throws BCOGraphQLError {
        try {
            final UnitRegistryRemote unitRegistry = Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);

            final UnitConfig.Builder unitConfigBuilder = unitRegistry.getUnitConfigById(unitId).toBuilder();
            final MetaConfigType.MetaConfig.Builder metaConfigBuilder = unitConfigBuilder.getMetaConfigBuilder();
            for (int i = 0; i < metaConfigBuilder.getEntryCount(); i++) {
                if (metaConfigBuilder.getEntry(i).getKey().equals(entry.getKey())) {
                    metaConfigBuilder.removeEntry(i);
                    break;
                }
            }
            if (!entry.getValue().isEmpty()) {
                metaConfigBuilder.addEntry(entry);
            }

            final SessionManager sessionManager = SessionManager.getInstance();
            final AuthToken authToken = AuthToken.newBuilder().setAuthenticationToken(((AbstractBCOGraphQLContext) env.getContext()).getToken()).build();
            final AuthenticatedValue request = sessionManager.initializeRequest(unitConfigBuilder.build(), authToken);
            final Future<AuthenticatedValue> future = unitRegistry.updateUnitConfigAuthenticated(request);
            final AuthenticatedValueFuture<UnitConfig> authFuture = new AuthenticatedValueFuture(future, UnitConfig.class, request.getTicketAuthenticatorWrapper(), sessionManager);
            return authFuture.get().getMetaConfig();
        } catch (RuntimeException | CouldNotPerformException | InterruptedException | ExecutionException ex) {
            throw new GenericError(ex);
        }
    }
}
