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
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.*;
import graphql.servlet.config.DefaultGraphQLSchemaProvider;
import graphql.servlet.config.GraphQLSchemaProvider;
import graphql.servlet.context.*;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.openbase.bco.api.graphql.batchloader.BCOUnitBatchLoader;
import org.openbase.bco.api.graphql.schema.LocationConfigSchemaModule;
import org.openbase.bco.api.graphql.schema.RegistrySchemaModule;
import org.openbase.bco.api.graphql.schema.UnitSchemaModule;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
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
                new LocationConfigSchemaModule(),
                new RegistrySchemaModule(),
                new UnitSchemaModule()
        );
    }

    @Bean
    public GraphQLSchemaProvider schemaProvider() {
        GraphQLSchema schema = injector.getInstance(Key.get(GraphQLSchema.class, Schema.class));
        return new DefaultGraphQLSchemaProvider(schema);
    }

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
    public DataLoaderRegistry buildDataLoaderRegistry(BCOUnitBatchLoader bcoUnitBatchLoader) {
        DataLoaderRegistry registry = new DataLoaderRegistry();
        registry.register("units", new DataLoader<>(bcoUnitBatchLoader));
        return registry;
    }

    @Bean
    public GraphQLContextBuilder contextBuilder(DataLoaderRegistry dataLoaderRegistry) {
        return new GraphQLContextBuilder() {

            @Override
            public GraphQLContext build(HttpServletRequest req, HttpServletResponse response) {
                return new AuthorizationContext(dataLoaderRegistry, req.getHeader("Authorization"));
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

    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {
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



    }


////
////    @Bean
////    public ServletRegistrationBean graphQLServlet() {
////
////    }
//
//    @Bean
//    GraphQLSchema schema() throws InterruptedException, CouldNotPerformException {
//
//        try {
//            Registries.waitUntilReady();
//
//            final URL url = Resources.getResource("schema.graphqls");
//            final String sdl = Resources.toString(url, Charsets.UTF_8);
//            final TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
//            final SchemaGenerator schemaGenerator = new SchemaGenerator();
//            final GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeRegistry, RuntimeWiring.newRuntimeWiring()
//                    .type(TypeRuntimeWiring.newTypeWiring("Query")
//                            .dataFetcher("bookById", graphQLDataFetchers.getBookByIdDataFetcher()))
//                    .type(TypeRuntimeWiring.newTypeWiring("Book")
//                            .dataFetcher("Book", graphQLDataFetchers.getAuthorDataFetcher()))
//                    .build());
//
//            return graphQLSchema;

//            final Builder schemaBuilder = GraphQLSchema.newSchema();

            // protobuf schema generator stuff
            /*ProtoUtils.
//            com.google.api.graphql.rejoiner.SchemaBundle
//            com.google.api.graphql.rejoiner.SchemaProviderModule*/

            // try to implement an code example without deprecated api usage. (see dataFetcher)
//            final GraphQLCodeRegistry.Builder codeRegistry = GraphQLCodeRegistry.newCodeRegistry();
//
//            codeRegistry.dataFetcher(dataFetchingEnvironment -> {
//
//            };
//
//            schemaBuilder.codeRegistry(codeRegistry.build());
//
//            // define root query
//            schemaBuilder.query(GraphQLObjectType.newObject()
//                    .name("query")
//                    .field(unit -> unit
//                            .name("unit")
//                            .type(Scalars.GraphQLString)
//                            .dataFetcher(environment -> "MR. Pink")
//                    )
//                    .build()
//            );

//            schemaBuilder.query(GraphQLObjectType.newObject()
//                            .name("query")
//                            .field(field -> field
//                                    .name("test")
//                                    .type(Scalars.GraphQLString)
//                                    .dataFetcher(environment -> "MR. Ping")
//                            )
//                            .build());
//            return schemaBuilder.build();

//        } catch (CouldNotPerformException | IOException ex) {
//            ExceptionPrinter.printHistory("Could not generate graphql schema!", ex, LOGGER);
//            throw new CouldNotPerformException("Could not generate graphql schema!", ex);
//        }
//    }

}
