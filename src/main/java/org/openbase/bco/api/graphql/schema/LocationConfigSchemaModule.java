package org.openbase.bco.api.graphql.schema;

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
