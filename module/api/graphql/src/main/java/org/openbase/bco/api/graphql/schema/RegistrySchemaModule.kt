package org.openbase.bco.api.graphql.schema

import com.google.api.graphql.rejoiner.*
import com.google.common.collect.ImmutableList
import graphql.schema.DataFetchingEnvironment
import org.openbase.bco.api.graphql.context.AbstractBCOGraphQLContext
import org.openbase.bco.api.graphql.error.ArgumentError
import org.openbase.bco.api.graphql.error.BCOGraphQLError
import org.openbase.bco.api.graphql.error.GenericError
import org.openbase.bco.api.graphql.error.ServerError
import org.openbase.bco.authentication.lib.SessionManager
import org.openbase.bco.authentication.lib.future.AuthenticatedValueFuture
import org.openbase.bco.authentication.lib.iface.BCOSession
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.remote.session.BCOSessionImpl
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor.mergeFromWithoutRepeatedFields
import org.openbase.jul.extension.type.processing.LabelProcessor.getBestMatch
import org.openbase.jul.extension.type.processing.LabelProcessor.replace
import org.openbase.type.configuration.EntryType
import org.openbase.type.configuration.MetaConfigType
import org.openbase.type.domotic.authentication.AuthTokenType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitFilterType
import org.openbase.type.domotic.unit.gateway.GatewayClassType
import org.openbase.type.geometry.PoseType
import org.openbase.type.language.LabelType
import org.openbase.type.spatial.PlacementConfigType
import org.openbase.type.spatial.ShapeType
import java.util.*
import java.util.concurrent.*

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
 */   class RegistrySchemaModule : SchemaModule() {
    /**
     * Check if an authentication token retrieved by the login method is still valid.
     *
     * @param token the token to be checked
     *
     * @return if the token is valid and can be used to authenticate further requests
     */
    @Query("verifyToken")
    fun verifyToken(@Arg("token") token: String?): java.lang.Boolean {
        TODO("blocked by https://github.com/openbase/bco.registry/issues/108")
    }

    /**
     * Method can be used to request a token of the given bco user.
     *
     * @param username     the name of the user as plain text string.
     * @param passwordHash the password hash of the user that need to be generted first (see note below).
     *
     * @return the login token.
     *
     *
     * Note: The hash of the default admin password is: '''R+gZ+PFuauhav8rRVa3XlWXXSEyi5BcdrbeXLEY3tDQ='''
     *
     * @throws BCOGraphQLError
     */
    @Query("login")
    @Throws(BCOGraphQLError::class)
    fun login(@Arg("username") username: String?, @Arg("password") passwordHash: String?): String {
        return try {
            val session: BCOSession = BCOSessionImpl()
            session.loginUserViaUsername(username, Base64.getDecoder().decode(passwordHash), false)
            session.generateAuthToken(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).authenticationToken
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Query("changePassword")
    @Throws(BCOGraphQLError::class)
    fun changePassword(
        @Arg("username") username: String?,
        @Arg("oldPassword") oldPassword: String?,
        @Arg("newPassword") newPassword: String?,
    ): String {
        try {
            val userId = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUserUnitIdByUserName(username)
            val sessionManager = SessionManager()
            try {
                sessionManager.loginUser(userId, oldPassword, false)
            } catch (ex: CouldNotPerformException) {
                throw ArgumentError(ex)
            }
            sessionManager.changePassword(
                userId,
                oldPassword,
                newPassword
            )[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT]
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
        return "läuft"
    }

    @Query("unitConfig")
    @Throws(BCOGraphQLError::class)
    fun getUnitConfigById(@Arg("id") id: String?, env: DataFetchingEnvironment?): UnitConfigType.UnitConfig {
        return try {
            Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUnitConfigById(id)
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        }
    }

    @Query("unitConfigs")
    @Throws(BCOGraphQLError::class)
    fun queryGetUnitConfigs(
        @Arg("filter") unitFilter: UnitFilterType.UnitFilter?,
        @Arg("includeDisabledUnits") includeDisabledUnits: Boolean?,
    ): ImmutableList<UnitConfigType.UnitConfig> {
        return getUnitConfigs(unitFilter, includeDisabledUnits)
    }

    // ===================================== Mutations =================================================================
    @Query("gatewayClasses")
    @Throws(CouldNotPerformException::class, InterruptedException::class)
    fun gatewayClasses(): ImmutableList<GatewayClassType.GatewayClass> {
        return ImmutableList.copyOf(Registries.getClassRegistry(true).gatewayClasses)
    }

    @Mutation("updateUnitConfig")
    @Throws(BCOGraphQLError::class)
    fun updateUnitConfig(@Arg("unitConfig") unitConfig: UnitConfigType.UnitConfig): UnitConfigType.UnitConfig {
        return try {
            val unitConfigBuilder = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            )
                .getUnitConfigById(unitConfig.id)
                .toBuilder()
            unitConfigBuilder.mergeFromWithoutRepeatedFields(unitConfig)
            Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            )
                .updateUnitConfig(unitConfigBuilder.build())[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT]
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Mutation("removeUnitConfig")
    @Throws(BCOGraphQLError::class)
    fun removeUnitConfig(@Arg("unitId") unitId: String?): UnitConfigType.UnitConfig {
        return try {
            val unitConfig = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUnitConfigById(unitId)
            Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            )
                .removeUnitConfig(unitConfig)[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT]
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Mutation("registerUnitConfig")
    @Throws(BCOGraphQLError::class)
    fun registerUnitConfig(@Arg("unitConfig") unitConfig: UnitConfigType.UnitConfig?): UnitConfigType.UnitConfig {
        return try {
            Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            )
                .registerUnitConfig(unitConfig)[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT]
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Mutation("updateLabel")
    @Throws(BCOGraphQLError::class)
    fun updateLabel(
        @Arg("unitId") unitId: String?,
        @Arg("label") label: String?,
        env: DataFetchingEnvironment,
    ): LabelType.Label {
        return try {
            val builder = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUnitConfigById(unitId).toBuilder()
            val context = env.getContext<AbstractBCOGraphQLContext>()
            val oldLabel = getBestMatch(context.languageCode!!, builder.label)
            replace(builder.labelBuilder, oldLabel, label)
            Registries.getUnitRegistry()
                .updateUnitConfig(builder.build())[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT].label
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Mutation("updateLocation")
    @Throws(BCOGraphQLError::class)
    fun updateLocation(
        @Arg("unitId") unitId: String?,
        @Arg("locationId") locationId: String?,
    ): PlacementConfigType.PlacementConfig {
        return try {
            val builder = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUnitConfigById(unitId).toBuilder()
            builder.placementConfigBuilder.locationId = locationId
            Registries.getUnitRegistry()
                .updateUnitConfig(builder.build())[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT].placementConfig
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Mutation("updateFloorPlan")
    @Throws(BCOGraphQLError::class)
    fun updateFloorPlan(@Arg("locationId") locationId: String?, @Arg("shape") shape: ShapeType.Shape): ShapeType.Shape {
        return try {
            val unitConfigBuilder = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUnitConfigById(locationId).toBuilder()
            unitConfigBuilder.placementConfigBuilder.shapeBuilder.clearFloor().addAllFloor(shape.floorList)
            Registries.getUnitRegistry()
                .updateUnitConfig(unitConfigBuilder.build())[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT].placementConfig.shape
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    @Mutation("updatePose")
    @Throws(BCOGraphQLError::class)
    fun updatePose(@Arg("unitId") unitId: String?, @Arg("pose") pose: PoseType.Pose?): PoseType.Pose {
        return try {
            val builder = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            ).getUnitConfigById(unitId).toBuilder()
            builder.placementConfigBuilder.clearPose().pose = pose
            Registries.getUnitRegistry()
                .updateUnitConfig(builder.build())[ServerError.Companion.BCO_TIMEOUT_SHORT, ServerError.Companion.BCO_TIMEOUT_TIME_UNIT].placementConfig.pose
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        } catch (ex: TimeoutException) {
            throw GenericError(ex)
        }
    }

    // ===================================== Service Methods ===========================================================
    @Mutation("updateMetaConfig")
    @Throws(BCOGraphQLError::class)
    fun updateMetaConfig(
        @Arg("unitId") unitId: String?,
        @Arg("entry") entry: EntryType.Entry,
        env: DataFetchingEnvironment,
    ): MetaConfigType.MetaConfig {
        return try {
            val unitRegistry = Registries.getUnitRegistry(
                ServerError.Companion.BCO_TIMEOUT_SHORT,
                ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
            )
            val unitConfigBuilder = unitRegistry.getUnitConfigById(unitId).toBuilder()
            val metaConfigBuilder = unitConfigBuilder.metaConfigBuilder
            for (i in 0 until metaConfigBuilder.entryCount) {
                if (metaConfigBuilder.getEntry(i).key == entry.key) {
                    metaConfigBuilder.removeEntry(i)
                    break
                }
            }
            if (!entry.value.isEmpty()) {
                metaConfigBuilder.addEntry(entry)
            }
            val sessionManager = SessionManager.getInstance()
            val authToken = AuthTokenType.AuthToken.newBuilder()
                .setAuthenticationToken((env.getContext<Any>() as AbstractBCOGraphQLContext).token).build()
            val request = sessionManager.initializeRequest(unitConfigBuilder.build(), authToken)
            val future = unitRegistry.updateUnitConfigAuthenticated(request)
            val authFuture: AuthenticatedValueFuture<UnitConfig> = AuthenticatedValueFuture(
                future,
                UnitConfig::class.java,
                request.ticketAuthenticatorWrapper,
                sessionManager
            )
            authFuture.get()!!.metaConfig
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        } catch (ex: ExecutionException) {
            throw GenericError(ex)
        }
    }

    companion object {
        // ===================================== Queries ===================================================================
        @Throws(BCOGraphQLError::class)
        fun getUnitConfigs(
            unitFilter: UnitFilterType.UnitFilter?,
            includeDisabledUnits: Boolean?,
        ): ImmutableList<UnitConfigType.UnitConfig> {
            var includeDisabledUnits: Boolean? = includeDisabledUnits
            return try {
                if (includeDisabledUnits == null) {
                    includeDisabledUnits = true
                }
                ImmutableList.copyOf(
                    Registries.getUnitRegistry(
                        ServerError.Companion.BCO_TIMEOUT_SHORT,
                        ServerError.Companion.BCO_TIMEOUT_TIME_UNIT
                    ).getUnitConfigs(includeDisabledUnits, unitFilter)
                )
            } catch (ex: RuntimeException) {
                throw GenericError(ex)
            } catch (ex: CouldNotPerformException) {
                throw GenericError(ex)
            } catch (ex: InterruptedException) {
                throw GenericError(ex)
            }
        }
    }
}
