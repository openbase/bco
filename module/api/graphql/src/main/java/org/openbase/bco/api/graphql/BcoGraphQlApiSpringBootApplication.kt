package org.openbase.bco.api.graphql

import com.google.api.graphql.execution.GuavaListenableFutureSupport
import com.google.api.graphql.rejoiner.*
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.Provides
import graphql.Scalars
import graphql.execution.instrumentation.Instrumentation
import graphql.kickstart.execution.context.DefaultGraphQLContext
import graphql.kickstart.execution.context.GraphQLKickstartContext
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder
import graphql.schema.*
import org.dataloader.DataLoader
import org.dataloader.DataLoaderRegistry
import org.openbase.bco.api.graphql.batchloader.BCOUnitBatchLoader
import org.openbase.bco.api.graphql.context.AbstractBCOGraphQLContext
import org.openbase.bco.api.graphql.context.BCOGraphQLWebsocketContext
import org.openbase.bco.api.graphql.context.DefaultBCOGraphQLContext
import org.openbase.bco.api.graphql.schema.RegistrySchemaModule
import org.openbase.bco.api.graphql.schema.SchemaModificationsAdd
import org.openbase.bco.api.graphql.schema.SchemaModificationsRemove
import org.openbase.bco.api.graphql.schema.UnitSchemaModule
import org.openbase.bco.api.graphql.subscriptions.SubscriptionModule
import org.openbase.bco.registry.remote.Registries
import org.openbase.bco.registry.unit.lib.UnitRegistry
import org.openbase.jul.exception.NotAvailableException
import org.openbase.type.domotic.unit.UnitFilterType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.websocket.Session
import javax.websocket.server.HandshakeRequest

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
 */@SpringBootApplication
open class BcoGraphQlApiSpringBootApplication {
    private var injector: Injector? = null

    @Value("\${graphql.url:/graphql}")
    private val graphqlurl: String? = null

    init {
        injector = Guice.createInjector(
            SchemaProviderModule(),  // WARNING:
            // The order of those two is important, see either class descriptions for details
            SchemaModificationsRemove(),
            SchemaModificationsAdd(),
            RegistrySchemaModule(),
            UnitSchemaModule()
        )
    }

    @Bean
    open fun schema(): GraphQLSchema {
        var schema = injector!!.getInstance(
            Key.get(
                GraphQLSchema::class.java, Schema::class.java
            )
        )
        val unitDataOutputType = schema.getType("openbase_type_domotic_unit_UnitData") as GraphQLOutputType?
        val unitConfigOutputType = schema.getType("openbase_type_domotic_unit_UnitConfig") as GraphQLOutputType?
        val unitFilterInputType = schema.getType("Input_openbase_type_domotic_unit_UnitFilter") as GraphQLInputType?
        val unitFilterInputConverter =
            GqlInputConverter.newBuilder().add(UnitFilterType.UnitFilter.getDescriptor().file).build()
        val builder = GraphQLObjectType.newObject().name("Subscription")
        builder.field(
            GraphQLFieldDefinition.newFieldDefinition().name("units").type(unitDataOutputType)
                .argument(GraphQLArgument.newArgument().name("filter").type(unitFilterInputType).build())
                .build()
        )
        builder.field(
            GraphQLFieldDefinition.newFieldDefinition().name("unitConfigs").type(GraphQLList.list(unitConfigOutputType))
                .argument(GraphQLArgument.newArgument().name("filter").type(unitFilterInputType))
                .argument(GraphQLArgument.newArgument().name("includeDisabledUnits").type(Scalars.GraphQLBoolean))
                .build()
        )
        val codeRegistry = GraphQLCodeRegistry.newCodeRegistry(schema.codeRegistry)
            .dataFetcher(FieldCoordinates.coordinates("Subscription", "units"), DataFetcher { dataFetchingEnvironment ->
                val unitFilter = unitFilterInputConverter.createProtoBuf(
                    UnitFilterType.UnitFilter.getDescriptor(),
                    UnitFilterType.UnitFilter.newBuilder(),
                    dataFetchingEnvironment.getArgument("filter")
                ) as UnitFilterType.UnitFilter
                SubscriptionModule.subscribeUnits(unitFilter)
            })
            .dataFetcher(
                FieldCoordinates.coordinates("Subscription", "unitConfigs"),
                DataFetcher { dataFetchingEnvironment ->
                    val unitFilter = unitFilterInputConverter.createProtoBuf(
                        UnitFilterType.UnitFilter.getDescriptor(),
                        UnitFilterType.UnitFilter.newBuilder(),
                        dataFetchingEnvironment.getArgument("filter")
                    ) as UnitFilterType.UnitFilter
                    var includeDisabledUnits = false
                    if (dataFetchingEnvironment.getArgument<Any>("includeDisabledUnits") != null) {
                        includeDisabledUnits = dataFetchingEnvironment.getArgument("includeDisabledUnits")
                    }
                    SubscriptionModule.subscribeUnitConfigs(unitFilter, includeDisabledUnits)
                })
            .build()
        return GraphQLSchema.newSchema(schema)
            .subscription(builder.build())
            .codeRegistry(codeRegistry)
            .build()
    }

    @Bean
    open fun instrumentation(): Instrumentation {
        return GuavaListenableFutureSupport.listenableFutureInstrumentation()
    }

    @Bean
    open fun unitRegistry(): UnitRegistry? {
        return try {
            Registries.getUnitRegistry()
        } catch (e: NotAvailableException) {
            e.printStackTrace()
            null
        }
    }

    @Bean
    @Provides
    open fun buildDataLoaderRegistry(bcoUnitBatchLoader: BCOUnitBatchLoader): DataLoaderRegistry {
        val registry = DataLoaderRegistry()
        registry.register(AbstractBCOGraphQLContext.Companion.DATA_LOADER_UNITS, DataLoader(bcoUnitBatchLoader))
        return registry
    }

    @Bean
    open fun contextBuilder(dataLoaderRegistry: DataLoaderRegistry): GraphQLServletContextBuilder {
        return object : GraphQLServletContextBuilder {
            override fun build(request: HttpServletRequest, response: HttpServletResponse): GraphQLKickstartContext {
                return DefaultBCOGraphQLContext(dataLoaderRegistry, request)
            }

            override fun build(): GraphQLKickstartContext {
                return DefaultGraphQLContext(dataLoaderRegistry, null)
            }

            override fun build(session: Session, request: HandshakeRequest): GraphQLKickstartContext {
                return BCOGraphQLWebsocketContext(
                    session = session,
                    handshakeRequest = request,
                    dataLoaderRegistry = dataLoaderRegistry
                )
            }
        }
    }
}
