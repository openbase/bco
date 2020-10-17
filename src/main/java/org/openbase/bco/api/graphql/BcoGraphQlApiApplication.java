package org.openbase.bco.api.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLSchema.Builder;
import io.grpc.protobuf.ProtoUtils;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BcoGraphQlApiApplication {

    private static Logger LOGGER = LoggerFactory.getLogger(BcoGraphQlApiApplication.class);

    public static void main(String[] args) throws InterruptedException, CouldNotPerformException {

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
//
//    @Bean
//    public ServletRegistrationBean graphQLServlet() {
//
//    }

    @Bean
    GraphQLSchema schema() throws InterruptedException, CouldNotPerformException {

        try {
            Registries.waitUntilReady();

            final Builder schemaBuilder = GraphQLSchema.newSchema();

            // protobuf schema generator stuff
//            ProtoUtils.
//            com.google.api.graphql.rejoiner.SchemaBundle
//            com.google.api.graphql.rejoiner.SchemaProviderModule

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

            schemaBuilder.query(GraphQLObjectType.newObject()
                            .name("query")
                            .field(field -> field
                                    .name("test")
                                    .type(Scalars.GraphQLString)
                                    .dataFetcher(environment -> "MR. Ping")
                            )
                            .build());
            return schemaBuilder.build();

        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not gerenarte grapql schema!", ex, LOGGER);
            throw ex;
        }
    }

}
