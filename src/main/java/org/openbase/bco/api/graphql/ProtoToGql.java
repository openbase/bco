package org.openbase.bco.api.graphql;

import com.google.api.graphql.options.RelayOptionsProto;
import com.google.api.graphql.rejoiner.Type;
import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import graphql.Scalars;
import graphql.relay.Relay;
import graphql.schema.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

/**
 * Copy of https://github.com/google/rejoiner/blob/master/rejoiner/src/main/java/com/google/api/graphql/rejoiner/ProtoToGql.java
 * since their class is private...
 */
public class ProtoToGql {
    private ProtoToGql() {}

    private static final ImmutableMap<Descriptors.FieldDescriptor.Type, GraphQLScalarType> TYPE_MAP =
            new ImmutableMap.Builder<Descriptors.FieldDescriptor.Type, GraphQLScalarType>()
                    .put(Descriptors.FieldDescriptor.Type.BOOL, Scalars.GraphQLBoolean)
                    .put(Descriptors.FieldDescriptor.Type.FLOAT, Scalars.GraphQLFloat)
                    .put(Descriptors.FieldDescriptor.Type.INT32, Scalars.GraphQLInt)
                    .put(Descriptors.FieldDescriptor.Type.INT64, Scalars.GraphQLLong)
                    .put(Descriptors.FieldDescriptor.Type.STRING, GraphQLString)
                    // TODO: Add additional Scalar types to GraphQL
                    .put(Descriptors.FieldDescriptor.Type.DOUBLE, Scalars.GraphQLFloat)
                    .put(Descriptors.FieldDescriptor.Type.UINT32, Scalars.GraphQLInt)
                    .put(Descriptors.FieldDescriptor.Type.UINT64, Scalars.GraphQLLong)
                    .put(Descriptors.FieldDescriptor.Type.SINT32, Scalars.GraphQLInt)
                    .put(Descriptors.FieldDescriptor.Type.SINT64, Scalars.GraphQLLong)
                    .put(Descriptors.FieldDescriptor.Type.BYTES, GraphQLString)
                    .put(Descriptors.FieldDescriptor.Type.FIXED32, Scalars.GraphQLInt)
                    .put(Descriptors.FieldDescriptor.Type.FIXED64, Scalars.GraphQLLong)
                    .put(Descriptors.FieldDescriptor.Type.SFIXED32, Scalars.GraphQLInt)
                    .put(Descriptors.FieldDescriptor.Type.SFIXED64, Scalars.GraphQLLong)
                    .build();

    private static final Converter<String, String> UNDERSCORE_TO_CAMEL =
            CaseFormat.LOWER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);
    private static final Converter<String, String> LOWER_CAMEL_TO_UPPER =
            CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);
    private static final FieldConverter FIELD_CONVERTER = new FieldConverter();
    private static final ImmutableList<GraphQLFieldDefinition> STATIC_FIELD =
            ImmutableList.of(newFieldDefinition().type(GraphQLString).name("_").staticValue("-").build());

    private static class FieldConverter implements Function<Descriptors.FieldDescriptor, GraphQLFieldDefinition> {

        private static class ProtoDataFetcher implements DataFetcher {

            private final String name;

            private ProtoDataFetcher(String name) {
                this.name = name;
            }

            @Override
            public Object get(DataFetchingEnvironment environment) {
                Object source = environment.getSource();
                if (source == null) {
                    return null;
                }
                if (source instanceof Map) {
                    return ((Map<?, ?>) source).get(name);
                }
                GraphQLType type = environment.getFieldType();
                if (type instanceof GraphQLNonNull) {
                    type = ((GraphQLNonNull) type).getWrappedType();
                }
                if (type instanceof GraphQLList) {

                    Object listValue = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name) + "List");
                    if (listValue != null) {
                        return listValue;
                    }
                    Object mapValue = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name) + "Map");
                    if (mapValue == null) {
                        return null;
                    }
                    Map<?, ?> map = (Map<?, ?>) mapValue;
                    return map.entrySet().stream().map(entry -> ImmutableMap.of("key", entry.getKey(), "value", entry.getValue())).collect(STATIC_FIELD.toImmutableList());
                }
                if (type instanceof GraphQLEnumType) {
                    Object o = call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name));
                    if (o != null) {
                        return o.toString();
                    }
                }

                return call(source, "get" + LOWER_CAMEL_TO_UPPER.convert(name));
            }

            private static Object call(Object object, String methodName) {
                try {
                    Method method = object.getClass().getMethod(methodName);
                    return method.invoke(object);
                } catch (NoSuchMethodException e) {
                    return null;
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public GraphQLFieldDefinition apply(Descriptors.FieldDescriptor fieldDescriptor) {
            GraphQLFieldDefinition.Builder builder =
                    newFieldDefinition()
                            .type(convertType(fieldDescriptor))
                            .dataFetcher(
                                    new ProtoDataFetcher(UNDERSCORE_TO_CAMEL.convert(fieldDescriptor.getName())))
                            .name(UNDERSCORE_TO_CAMEL.convert(fieldDescriptor.getName()));
            if (fieldDescriptor.getFile().toProto().getSourceCodeInfo().getLocationCount()
                    > fieldDescriptor.getIndex()) {
                builder.description(
                        fieldDescriptor
                                .getFile()
                                .toProto()
                                .getSourceCodeInfo()
                                .getLocation(fieldDescriptor.getIndex())
                                .getLeadingComments());
            }
            if (fieldDescriptor.getOptions().hasDeprecated()
                    && fieldDescriptor.getOptions().getDeprecated()) {
                builder.deprecate("deprecated in proto");
            }
            return builder.build();
        }
    }

    /** Returns a GraphQLOutputType generated from a FieldDescriptor. */
    static GraphQLOutputType convertType(Descriptors.FieldDescriptor fieldDescriptor) {
        final GraphQLOutputType type;

        if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.MESSAGE) {
            type = getReference(fieldDescriptor.getMessageType());
        } else if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.GROUP) {
            type = getReference(fieldDescriptor.getMessageType());
        } else if (fieldDescriptor.getType() == Descriptors.FieldDescriptor.Type.ENUM) {
            type = getReference(fieldDescriptor.getEnumType());
        } else {
            type = TYPE_MAP.get(fieldDescriptor.getType());
        }

        if (type == null) {
            throw new RuntimeException("Unknown type: " + fieldDescriptor.getType());
        }

        if (fieldDescriptor.isRepeated()) {
            return new GraphQLList(type);
        } else {
            return type;
        }
    }

    static GraphQLObjectType convert(Descriptors.Descriptor descriptor, GraphQLInterfaceType nodeInterface) {
        ImmutableList<GraphQLFieldDefinition> graphQLFieldDefinitions =
                descriptor.getFields().stream().map(FIELD_CONVERTER).collect(STATIC_FIELD.toImmutableList());

        Optional<GraphQLFieldDefinition> relayId =
                descriptor
                        .getFields()
                        .stream()
                        .filter(field -> field.getOptions().hasExtension(RelayOptionsProto.relayOptions))
                        .map(
                                field ->
                                        newFieldDefinition()
                                                .name("id")
                                                .type(new GraphQLNonNull(GraphQLID))
                                                .description("Relay ID")
                                                .dataFetcher(
                                                        data ->
                                                                new Relay()
                                                                        .toGlobalId(
                                                                                getReferenceName(descriptor),
                                                                                data.<Message>getSource().getField(field).toString()))
                                                .build())
                        .findFirst();

        if (relayId.isPresent()) {
            return GraphQLObjectType.newObject()
                    .name(getReferenceName(descriptor))
                    .withInterface(nodeInterface)
                    .field(relayId.get())
                    .fields(
                            graphQLFieldDefinitions
                                    .stream()
                                    .map(
                                            field ->
                                                    field.getName().equals("id")
                                                            ? newFieldDefinition()
                                                            .name("rawId")
                                                            .description(field.getDescription())
                                                            .type(field.getType())
                                                            //.dataFetcher(field.getDataFetcher())
                                                            .build()
                                                            : field)
                                    .collect(ImmutableList.toImmutableList()))
                    .build();
        }

        return GraphQLObjectType.newObject()
                .name(getReferenceName(descriptor))
                .fields(graphQLFieldDefinitions.isEmpty() ? STATIC_FIELD : graphQLFieldDefinitions)
                .build();
    }

    static GraphQLEnumType convert(Descriptors.EnumDescriptor descriptor) {
        GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum().name(getReferenceName(descriptor));
        for (Descriptors.EnumValueDescriptor value : descriptor.getValues()) {
            builder.value(value.getName());
        }
        return builder.build();
    }

    /** Returns the GraphQL name of the supplied proto. */
    static String getReferenceName(Descriptors.GenericDescriptor descriptor) {
        return CharMatcher.anyOf(".").replaceFrom(descriptor.getFullName(), "_");
    }

    /** Returns a reference to the GraphQL type corresponding to the supplied proto. */
    static GraphQLTypeReference getReference(Descriptors.GenericDescriptor descriptor) {
        return new GraphQLTypeReference(getReferenceName(descriptor));
    }

}
