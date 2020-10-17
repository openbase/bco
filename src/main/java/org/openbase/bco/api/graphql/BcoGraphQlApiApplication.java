package org.openbase.bco.api.graphql;

import com.google.common.io.Resources;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.*;
import graphql.schema.GraphQLSchema.Builder;
import graphql.schema.idl.*;
import io.grpc.protobuf.ProtoUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOExceptionList;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.net.URL;

@SpringBootApplication
public class BcoGraphQlApiApplication {

    private static Logger LOGGER = LoggerFactory.getLogger(BcoGraphQlApiApplication.class);

    @Autowired
    GraphQLDataFetchers graphQLDataFetchers;

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

        BCO.printLogo();
        LOGGER.info("Connect to bco...");
        Registries.waitUntilReady();

        LOGGER.info("Start webserver...");
        SpringApplication.run(BcoGraphQlApiApplication.class, args);

        LOGGER.info("Wait for application termination...");
        final String s = new String();
        synchronized (s) {
            s.wait();
        }

        System.out.println("done.");

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
            com.google.api.graphql.rejoiner.SchemaBundle
            com.google.api.graphql.rejoiner.SchemaProviderModule*/

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
