package org.openbase.bco.api.graphql.subscriptions

import com.google.protobuf.Message
import io.reactivex.BackpressureStrategy
import org.openbase.bco.api.graphql.error.BCOGraphQLError
import org.openbase.bco.api.graphql.error.GenericError
import org.openbase.bco.api.graphql.error.ServerError
import org.openbase.bco.dal.lib.layer.unit.Unit
import org.openbase.bco.dal.lib.layer.unit.UnitRemote
import org.openbase.bco.dal.remote.layer.unit.CustomUnitPool
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.extension.protobuf.ProtoBufBuilderProcessor.merge
import org.openbase.jul.pattern.Observer
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.type.domotic.communication.UserMessageType.UserMessage
import org.openbase.type.domotic.registry.MessageRegistryDataType.MessageRegistryData
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig
import org.openbase.type.domotic.unit.UnitDataType
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory

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
object SubscriptionModule {
    private val log = LoggerFactory.getLogger(SubscriptionModule::class.java)

    //TODO: what is a good strategy here
    private val BACKPRESSURE_STRATEGY = BackpressureStrategy.BUFFER

    @Throws(BCOGraphQLError::class)
    fun subscribeUnits(unitFilter: UnitFilter): Publisher<UnitDataType.UnitData> {
        return try {
            val subscriptionUnitPool = CustomUnitPool<Message, UnitRemote<Message>>()
            subscriptionUnitPool.init(unitFilter)
            AbstractObserverMapper.createObservable(
                { observer: Observer<Unit<Message>, Message> ->
                    subscriptionUnitPool.addDataObserver(
                        observer
                    )
                },
                { observer: Observer<Unit<Message>, Message> ->
                    subscriptionUnitPool.removeDataObserver(observer)
                },
                object : AbstractObserverMapper<Unit<Message>, Message, UnitDataType.UnitData>() {
                    override fun mapData(source: Unit<Message>, data: Message): UnitDataType.UnitData {
                        return merge(UnitDataType.UnitData.newBuilder(), data!!).build() as UnitDataType.UnitData
                    }

                    @Throws(CouldNotPerformException::class, InterruptedException::class)
                    override fun doAfterAddObserver() {
                        subscriptionUnitPool.activate()
                        for (unitRemote in subscriptionUnitPool.internalUnitList) {
                            log.debug("Subscribe to: $unitRemote")
                        }
                    }

                    override fun doAfterRemoveObserver() {
                        subscriptionUnitPool.shutdown()
                    }
                }).toFlowable(BACKPRESSURE_STRATEGY)
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        }
    }

    @Throws(BCOGraphQLError::class)
    fun subscribeUnitConfigs(
        unitFilter: UnitFilter,
        includeDisabledUnits: Boolean,
    ): Publisher<List<UnitConfig>> {
        return try {
            val observer = UnitRegistrySubscriptionObserver(unitFilter, includeDisabledUnits)
            val unitRegistry = Registries.getUnitRegistry(
                ServerError.BCO_TIMEOUT_SHORT,
                ServerError.BCO_TIMEOUT_TIME_UNIT
            )
            AbstractObserverMapper.createObservable(
                { observer: Observer<DataProvider<UnitRegistryData>, UnitRegistryData> ->
                    unitRegistry.addDataObserver(
                        observer
                    )
                },
                { observer: Observer<DataProvider<UnitRegistryData>, UnitRegistryData> ->
                    unitRegistry.removeDataObserver(observer)
                },
                observer
            ).toFlowable(BACKPRESSURE_STRATEGY)
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        }
    }

    @Throws(BCOGraphQLError::class)
    fun subscribeUserMessages(): Publisher<List<UserMessage>> {
        return try {
            val observer = MessageRegistrySubscriptionObserver()
            val messageRegistry = Registries.getMessageRegistry(
                ServerError.BCO_TIMEOUT_SHORT,
                ServerError.BCO_TIMEOUT_TIME_UNIT
            )
            AbstractObserverMapper.createObservable(
                { observer: Observer<DataProvider<MessageRegistryData>, MessageRegistryData> ->
                    messageRegistry.addDataObserver(
                        observer
                    )
                },
                { observer: Observer<DataProvider<MessageRegistryData>, MessageRegistryData> ->
                    messageRegistry.removeDataObserver(observer)
                },
                observer
            ).toFlowable(BACKPRESSURE_STRATEGY)
        } catch (ex: RuntimeException) {
            throw GenericError(ex)
        } catch (ex: CouldNotPerformException) {
            throw GenericError(ex)
        } catch (ex: InterruptedException) {
            throw GenericError(ex)
        }
    }
}
