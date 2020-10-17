package org.openbase.bco.api.graphql.schema;

import com.google.api.graphql.rejoiner.SchemaModification;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.api.graphql.rejoiner.Type;
import com.google.api.graphql.rejoiner.TypeModification;
import com.google.common.util.concurrent.ListenableFuture;
import graphql.schema.DataFetchingEnvironment;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;

import java.util.List;

public class LocationConfigSchemaModule extends SchemaModule {

    @SchemaModification
    TypeModification removeBookIds = Type.find(LocationConfig.getDescriptor()).removeField("unitId");

    @SchemaModification(addField = "units", onType = LocationConfig.class)
    ListenableFuture<List<UnitConfig>> unitIdToUnitConfig(LocationConfig locationConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(locationConfig.getUnitIdList()));
    }
}