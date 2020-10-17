package org.openbase.bco.api.graphql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class GraphQLProvider {

    private static Logger LOGGER = LoggerFactory.getLogger(GraphQLDataFetchers.class);

    @Autowired
    GraphQLDataFetchers graphQLDataFetchers;

    private GraphQL graphQL;

    @PostConstruct
    public void init() throws IOException {
        LOGGER.info("Init GraphQLProvider ...");
        URL url = Resources.getResource("schema.graphqls");
        String sdl = Resources.toString(url, Charsets.UTF_8);
        LOGGER.info("SDL: " + sdl);
        GraphQLSchema graphQLSchema = buildSchema(sdl);

        for (GraphQLType graphQLType : graphQLSchema.getAllTypesAsList()) {
            LOGGER.info("Types {}", graphQLType);
        }

        LOGGER.info("Test {}", graphQLSchema.getQueryType());
        this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
    }

    public GraphQLSchema buildSchema() {
        URL url = Resources.getResource("schema.graphqls");
        String sdl = null;
        try {
            sdl = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buildSchema(sdl);
    }

    public GraphQLSchema buildSchema(String sdl) {
        LOGGER.info("Build schema...");
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
//        GraphQLDataFetchers graphQLDataFetchers = new GraphQLDataFetchers();
        LOGGER.info("Build wiring...");
        return RuntimeWiring.newRuntimeWiring()
                .type(newTypeWiring("Query")
                        .dataFetcher("bookById", graphQLDataFetchers.getBookByIdDataFetcher()))
                .type(newTypeWiring("Book")
                        .dataFetcher("author", graphQLDataFetchers.getAuthorDataFetcher()))
                .build();
    }

    @Bean
    public GraphQL graphQL() {
        LOGGER.info("Return graphQL...");
        return graphQL;
    }
}
