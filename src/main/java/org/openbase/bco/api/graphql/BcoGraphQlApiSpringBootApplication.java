package org.openbase.bco.api.graphql;

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

import com.google.api.graphql.execution.GuavaListenableFutureSupport;
import com.google.api.graphql.rejoiner.Schema;
import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.inject.*;
import graphql.execution.instrumentation.Instrumentation;
import graphql.kickstart.execution.context.DefaultGraphQLContext;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.servlet.context.DefaultGraphQLWebSocketContext;
import graphql.kickstart.servlet.context.GraphQLServletContextBuilder;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.openbase.bco.api.graphql.batchloader.BCOUnitBatchLoader;
import org.openbase.bco.api.graphql.schema.RegistrySchemaModule;
import org.openbase.bco.api.graphql.schema.SchemaModificationsAdd;
import org.openbase.bco.api.graphql.schema.SchemaModificationsRemove;
import org.openbase.bco.api.graphql.schema.UnitSchemaModule;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;

@SpringBootApplication
public class BcoGraphQlApiSpringBootApplication {

    private static Logger LOGGER = LoggerFactory.getLogger(BcoGraphQlApiSpringBootApplication.class);

    private final Injector injector;

    {
        injector = Guice.createInjector(
                new SchemaProviderModule(),

                // WARNING:
                // The order of those two is important, see either class descriptions for details
                new SchemaModificationsRemove(),
                new SchemaModificationsAdd(),

                new RegistrySchemaModule(),
                new UnitSchemaModule()
        );
    }

    @Bean
    GraphQLSchema schema() {
        GraphQLSchema schema = injector.getInstance(Key.get(GraphQLSchema.class, Schema.class));

        final GraphQLObjectType.Builder queryTypeBuilder = GraphQLObjectType.newObject(schema.getQueryType());
        final GraphQLObjectType.Builder mutationTypeBuilder = GraphQLObjectType.newObject(schema.getMutationType());
        //TODO: can I define that these arguments can not be null as in an SDL
        //TODO: would the preferred way be to define these in an sdl?
        /*queryTypeBuilder.field(GraphQLFieldDefinition.newFieldDefinition().name("login").type(Scalars.GraphQLString)
                .argument(GraphQLArgument.newArgument().name("username").type(GraphQLNonNull.nonNull(Scalars.GraphQLString)).build())
                .argument(GraphQLArgument.newArgument().name("password").type(GraphQLNonNull.nonNull(Scalars.GraphQLString)).build())
                .build());
        mutationTypeBuilder.field(GraphQLFieldDefinition.newFieldDefinition().name("changePassword").type(Scalars.GraphQLBoolean)
                .argument(GraphQLArgument.newArgument().name("username").type(GraphQLNonNull.nonNull(Scalars.GraphQLString)).build())
                .argument(GraphQLArgument.newArgument().name("oldPassword").type(GraphQLNonNull.nonNull(Scalars.GraphQLString)).build())
                .argument(GraphQLArgument.newArgument().name("newPassword").type(GraphQLNonNull.nonNull(Scalars.GraphQLString)).build())
                .build());

        final GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry(schema.getCodeRegistry())
                .dataFetcher(FieldCoordinates.coordinates(schema.getQueryType().getName(), "login"), new DataFetcher<String>() {

                    @Override
                    public String get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
                        final String username = dataFetchingEnvironment.getArgument("username");
                        final String password = dataFetchingEnvironment.getArgument("password");

                        try {
                            final String userId = Registries.getUnitRegistry().getUserUnitIdByUserName(username);
                            final SessionManager sessionManager = new SessionManager();
                            sessionManager.loginUser(userId, password, false);
                            AuthenticatedValueType.AuthenticatedValue authenticatedValue = sessionManager.initializeRequest(AuthenticationTokenType.AuthenticationToken.newBuilder().setUserId(userId).build(), null);
                            String tokenValue = new AuthenticatedValueFuture<>(Registries.getUnitRegistry().requestAuthenticationTokenAuthenticated(authenticatedValue),
                                    String.class,
                                    authenticatedValue.getTicketAuthenticatorWrapper(),
                                    sessionManager).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);
                            return tokenValue;
                        } catch (NotAvailableException ex) {

                            throw new ArgumentError(ex);
                        } catch (Throwable ex) {
                            System.out.println("Which ex is thrown here? " + ex.getClass().getSimpleName() + ", " + ex.getMessage());
                            throw new Exception(ex);
                        }
                    }
                })
                .dataFetcher(FieldCoordinates.coordinates(schema.getMutationType().getName(), "changePassword"), new DataFetcher<Boolean>() {
                    @Override
                    public Boolean get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
                        final String username = dataFetchingEnvironment.getArgument("username");
                        final String oldPassword = dataFetchingEnvironment.getArgument("oldPassword");
                        final String newPassword = dataFetchingEnvironment.getArgument("newPassword");

                        final String userId = Registries.getUnitRegistry().getUserUnitIdByUserName(username);

                        final SessionManager sessionManager = new SessionManager();
                        sessionManager.loginUser(userId, oldPassword, false);
                        sessionManager.changePassword(userId, oldPassword, newPassword).get(ServerError.BCO_TIMEOUT_SHORT, ServerError.BCO_TIMEOUT_TIME_UNIT);

                        return true;
                    }
                })
                .build();

        schema = GraphQLSchema.newSchema(schema)
                .query(queryTypeBuilder.build())
                .mutation(mutationTypeBuilder.build())
                .codeRegistry(codeRegistry)
                .build();*/
        return schema;
    }
//
//    @Bean
//    public GraphQL graphQL() {
//        return GraphQL.newGraphQL(schemaProvider().getSchema()).build();
//    }

    @Bean
    public Instrumentation instrumentation() {
        return GuavaListenableFutureSupport.listenableFutureInstrumentation();
    }

    @Bean
    public UnitRegistry unitRegistry() {
        try {
            return Registries.getUnitRegistry();
        } catch (NotAvailableException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Bean
    @Provides
    public DataLoaderRegistry buildDataLoaderRegistry(BCOUnitBatchLoader bcoUnitBatchLoader) {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register(BCOGraphQLContext.DATA_LOADER_UNITS, new DataLoader<>(bcoUnitBatchLoader));
        return registry;
    }

    @Bean
    public GraphQLServletContextBuilder contextBuilder(DataLoaderRegistry dataLoaderRegistry) {
        return new GraphQLServletContextBuilder() {

            @Override
            public GraphQLContext build(HttpServletRequest request, HttpServletResponse response) {
                return new BCOGraphQLContext(dataLoaderRegistry, null, request);
            }

            @Override
            public GraphQLContext build() {
                return new DefaultGraphQLContext(dataLoaderRegistry, null);
            }

            @Override
            public GraphQLContext build(Session session, HandshakeRequest request) {
                return DefaultGraphQLWebSocketContext.createWebSocketContext(dataLoaderRegistry, null).with(session).with(request).build();
            }
        };
    }


//    @Autowired
//    GraphQLDataFetchers graphQLDataFetchers;

//    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {
//        String schema = "type Query{hello: String}";
//
//        SchemaParser schemaParser = new SchemaParser();
//        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);
//
//        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
//                .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
//                .build();
//
//        SchemaGenerator schemaGenerator = new SchemaGenerator();
//        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);
//
//        GraphQL build = GraphQL.newGraphQL(new GraphQLProvider().buildSchema()).build();
//        ExecutionResult executionResult = build.execute("{bookById(id: \"book-1\"){name}}");
//
//        System.out.println(executionResult.getData().toString());

//    }

}
