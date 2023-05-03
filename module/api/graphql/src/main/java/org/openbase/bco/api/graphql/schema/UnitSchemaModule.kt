package org.openbase.bco.api.graphql.schema

import com.google.api.graphql.rejoiner.*
import com.google.common.collect.ImmutableList
import graphql.schema.DataFetchingEnvironment
import org.openbase.bco.api.graphql.context.AbstractBCOGraphQLContext
import org.openbase.bco.api.graphql.error.BCOGraphQLError
import org.openbase.bco.api.graphql.error.GenericError
import org.openbase.bco.api.graphql.error.ServerError
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor
import org.openbase.bco.dal.lib.layer.service.Services
import org.openbase.bco.dal.remote.action.RemoteAction
import org.openbase.bco.dal.remote.layer.unit.Units
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.exception.NotAvailableException
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor.merge
import org.openbase.type.domotic.authentication.AuthTokenType
import org.openbase.type.domotic.service.ServiceTempusTypeType
import org.openbase.type.domotic.unit.UnitConfigType
import org.openbase.type.domotic.unit.UnitDataType
import org.openbase.type.domotic.unit.UnitFilterType
import java.util.concurrent.TimeUnit

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
class UnitSchemaModule : SchemaModule() {
    // ===================================== Schema Modifications ======================================================
    @SchemaModification(addField = "config", onType = UnitDataType.UnitData::class)
    @Throws(CouldNotPerformException::class, InterruptedException::class)
    fun addConfigToData(data: UnitDataType.UnitData): UnitConfigType.UnitConfig = Registries.getUnitRegistry(
        ServerError.BCO_TIMEOUT_SHORT,
        ServerError.BCO_TIMEOUT_TIME_UNIT
    ).getUnitConfigById(data.id)

    // ===================================== Queries ===================================================================
    @Query("unit")
    @Throws(BCOGraphQLError::class)
    fun unit(@Arg("unitId") id: String?): UnitDataType.UnitData = try {
        Units
            .getUnit(id, ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT)
            .let { unit -> merge(UnitDataType.UnitData.newBuilder(), unit.data).build() as UnitDataType.UnitData }
    } catch (ex: RuntimeException) {
        throw GenericError(ex)
    } catch (ex: CouldNotPerformException) {
        throw GenericError(ex)
    } catch (ex: InterruptedException) {
        throw GenericError(ex)
    }

    @Query("units")
    @Throws(BCOGraphQLError::class)
    fun units(@Arg("filter") unitFilter: UnitFilterType.UnitFilter?): ImmutableList<UnitDataType.UnitData> = try {
        val dataList: MutableList<UnitDataType.UnitData> = ArrayList()
        for (unitConfig in Registries.getUnitRegistry(
            ServerError.BCO_TIMEOUT_SHORT,
            ServerError.BCO_TIMEOUT_TIME_UNIT
        ).getUnitConfigs(unitFilter)) {
            val unit = Units.getUnit(
                unitConfig,
                ServerError.BCO_TIMEOUT_SHORT,
                ServerError.BCO_TIMEOUT_TIME_UNIT
            )
            dataList.add(merge(UnitDataType.UnitData.newBuilder(), unit.data).build() as UnitDataType.UnitData)
        }
        ImmutableList.copyOf(dataList)
    } catch (ex: RuntimeException) {
        throw GenericError(ex)
    } catch (ex: CouldNotPerformException) {
        throw GenericError(ex)
    } catch (ex: InterruptedException) {
        throw GenericError(ex)
    }

    // ===================================== Queries ===================================================================
    @Mutation("unit")
    @Throws(BCOGraphQLError::class)
    fun unit(
        @Arg("unitId") unitId: String,
        @Arg("data") data: UnitDataType.UnitData,
        env: DataFetchingEnvironment,
    ): UnitDataType.UnitData = try {
        setServiceStates(unitId, data, env)
    } catch (ex: RuntimeException) {
        throw GenericError(ex)
    } catch (ex: CouldNotPerformException) {
        throw GenericError(ex)
    } catch (ex: InterruptedException) {
        throw GenericError(ex)
    }

    @Mutation("units")
    @Throws(BCOGraphQLError::class)
    fun units(
        @Arg("filter") unitFilter: UnitFilterType.UnitFilter?,
        @Arg("data") data: UnitDataType.UnitData,
        env: DataFetchingEnvironment,
    ): ImmutableList<UnitDataType.UnitData> = try {
        Registries.getUnitRegistry(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT)
            .getUnitConfigs(unitFilter)
            .map { config -> setServiceStates(config, data, env) }
            .let { ImmutableList.copyOf(it) }
    } catch (ex: RuntimeException) {
        throw GenericError(ex)
    } catch (ex: CouldNotPerformException) {
        throw GenericError(ex)
    } catch (ex: InterruptedException) {
        throw GenericError(ex)
    }

    // ===================================== Service Methods ===========================================================
    @Throws(CouldNotPerformException::class, InterruptedException::class)
    private fun setServiceStates(
        unitConfig: UnitConfigType.UnitConfig,
        data: UnitDataType.UnitData,
        env: DataFetchingEnvironment,
        timeout: Long = ServerError.BCO_TIMEOUT_SHORT,
        timeUnit: TimeUnit = ServerError.BCO_TIMEOUT_TIME_UNIT,
    ): UnitDataType.UnitData {
        return setServiceStates(unitConfig.id, data, env, timeout, timeUnit)
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    private fun setServiceStates(
        unitId: String,
        data: UnitDataType.UnitData,
        env: DataFetchingEnvironment,
        timeout: Long = ServerError.BCO_TIMEOUT_SHORT,
        timeUnit: TimeUnit = ServerError.BCO_TIMEOUT_TIME_UNIT,
    ): UnitDataType.UnitData {
        val unit = Units.getUnit(unitId, timeout, timeUnit)
        val remoteActions: MutableList<RemoteAction> = ArrayList()
        for (serviceType in unit.supportedServiceTypes) {
            if (!Services.hasServiceState(
                    serviceType,
                    ServiceTempusTypeType.ServiceTempusType.ServiceTempus.CURRENT,
                    data
                )
            ) {
                continue
            }
            val serviceState = Services.invokeProviderServiceMethod(serviceType, data)
            val builder = ActionDescriptionProcessor.generateDefaultActionParameter(serviceState, serviceType, unit)
            try {
                builder.authToken = AuthTokenType.AuthToken.newBuilder()
                    .setAuthenticationToken((env.getContext<Any>() as AbstractBCOGraphQLContext).token).build()
            } catch (ex: NotAvailableException) {
                // in case the auth token is not available, we just continue without any authentication.
            }
            remoteActions.add(RemoteAction(unit.applyAction(builder), builder.authToken))
        }
        val unitDataBuilder = UnitDataType.UnitData.newBuilder()
        // TODO: blocked by https://github.com/openbase/bco.dal/issues/170
        for (remoteAction in remoteActions) {
            remoteAction.waitForRegistration(
                ServerError.BCO_TIMEOUT_SHORT,
                ServerError.BCO_TIMEOUT_TIME_UNIT
            )
            unitDataBuilder.addTriggeredAction(remoteAction.actionDescription)
        }
        merge(unitDataBuilder, unit.data)
        return unitDataBuilder.build()
    }
}
