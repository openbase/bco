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

import com.google.api.graphql.rejoiner.Arg;
import com.google.api.graphql.rejoiner.Mutation;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import com.google.common.collect.ImmutableList;
import graphql.schema.DataFetchingEnvironment;
import org.openbase.bco.api.graphql.BCOGraphQLContext;
import org.openbase.bco.api.graphql.error.ArgumentError;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.ExceptionProcessor;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.ListFilter;
import org.openbase.type.configuration.EntryType;
import org.openbase.type.configuration.MetaConfigType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitFilterType.UnitFilter;
import org.openbase.type.geometry.PoseType;
import org.openbase.type.language.LabelType;
import org.openbase.type.spatial.PlacementConfigType;
import org.openbase.type.spatial.ShapeType;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RegistrySchemaModule extends SchemaModule {

    @Query("unitConfig")
    UnitConfig getUnitConfigById(@Arg("id") String id, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException, ArgumentError {
        try {
            return Registries.getUnitRegistry(true).getUnitConfigById(id);
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    @Query("unitConfigs")
    ImmutableList<UnitConfig> getUnitConfigs(@Arg("filter") UnitFilter unitFilter, @Arg("inclusiveDisabled") Boolean incluseDisabled) throws CouldNotPerformException, InterruptedException {

        // setup default values
        if ((unitFilter == null)) {
            unitFilter = UnitFilter.getDefaultInstance();
        }
        if ((incluseDisabled == null)) {
            incluseDisabled = false;
        }

        return ImmutableList.copyOf(
                new UnitFilterImpl(unitFilter)
                        .pass(Registries.getUnitRegistry(true).getUnitConfigsFiltered(!incluseDisabled)));
    }

    @Mutation("updateUnitConfig")
    UnitConfig updateUnitConfig(@Arg("unitConfig") UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException, TimeoutException, ArgumentError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(true).getUnitConfigById(unitConfig.getId()).toBuilder();
            builder.mergeFrom(unitConfig);
            return Registries.getUnitRegistry(true).updateUnitConfig(unitConfig).get(5, TimeUnit.SECONDS);
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        } catch (ExecutionException ex) {
            throw new ArgumentError(ExceptionProcessor.getInitialCause(ex));
        }
    }

    @Mutation("removeUnitConfig")
    UnitConfig removeUnitConfig(@Arg("unitId") String unitId) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException, ArgumentError {
        try {
            final UnitConfig unitConfig = Registries.getUnitRegistry(true).getUnitConfigById(unitId);
            return Registries.getUnitRegistry(true).removeUnitConfig(unitConfig).get(5, TimeUnit.SECONDS);
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    @Mutation("registerUnitConfig")
    UnitConfig registerUnitConfig(@Arg("unitConfig") UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException, TimeoutException, ArgumentError {
        try {
            return Registries.getUnitRegistry(true).registerUnitConfig(unitConfig).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException ex) {
            throw new ArgumentError(ExceptionProcessor.getInitialCause(ex));
        }
    }

    @Mutation("updateLabel")
    LabelType.Label updateLabel(@Arg("unitId") String unitId, @Arg("label") String label, DataFetchingEnvironment env) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException, ArgumentError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(true).getUnitConfigById(unitId).toBuilder();

            final BCOGraphQLContext context = env.getContext();
            final String oldLabel = LabelProcessor.getBestMatch(context.getLanguageCode(), builder.getLabel());
            LabelProcessor.replace(builder.getLabelBuilder(), oldLabel, label);

            return Registries.getUnitRegistry().updateUnitConfig(builder.build()).get(5, TimeUnit.SECONDS).getLabel();
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    @Mutation("updateLocation")
    PlacementConfigType.PlacementConfig updateLocation(@Arg("unitId") String unitId, @Arg("locationId") String locationId) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException, ArgumentError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(true).getUnitConfigById(unitId).toBuilder();
            builder.getPlacementConfigBuilder().setLocationId(locationId);
            return Registries.getUnitRegistry().updateUnitConfig(builder.build()).get(5, TimeUnit.SECONDS).getPlacementConfig();
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    @Mutation("updateFloorPlan")
    ShapeType.Shape updateFloorPlan(@Arg("locationId") String locationId, @Arg("shape") ShapeType.Shape shape) throws CouldNotPerformException, InterruptedException, ExecutionException, TimeoutException, ArgumentError {
        try {
            final UnitConfig.Builder unitConfigBuilder = Registries.getUnitRegistry(true).getUnitConfigById(locationId).toBuilder();
            unitConfigBuilder.getPlacementConfigBuilder().getShapeBuilder().clearFloor().addAllFloor(shape.getFloorList());
            return Registries.getUnitRegistry().updateUnitConfig(unitConfigBuilder.build()).get(5, TimeUnit.SECONDS).getPlacementConfig().getShape();
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    @Mutation("updatePose")
    PoseType.Pose updatePose(@Arg("unitId") String unitId, @Arg("pose") PoseType.Pose pose) throws CouldNotPerformException, InterruptedException, TimeoutException, ExecutionException, ArgumentError {
        try {
            final UnitConfig.Builder builder = Registries.getUnitRegistry(true).getUnitConfigById(unitId).toBuilder();
            builder.getPlacementConfigBuilder().clearPose().setPose(pose);
            return Registries.getUnitRegistry().updateUnitConfig(builder.build()).get(5, TimeUnit.SECONDS).getPlacementConfig().getPose();
        } catch (NotAvailableException ex) {
            throw new ArgumentError(ex);
        }
    }

    /**
     * Check if an authentication token retrieved by the login method is still valid.
     *
     * @param token the token to be checked
     * @return if the token is valid and can be used to authenticate further requests
     */
    @Query("verifyToken")
    Boolean verifyToken(@Arg("token") String token) {
        //TODO: blocked by https://github.com/openbase/bco.registry/issues/108
        return true;
    }

    @Mutation("updateMetaConfig")
    MetaConfigType.MetaConfig updateMetaConfig(@Arg("unitId") String unitId, @Arg("entry") EntryType.Entry entry) throws NotAvailableException, InterruptedException, ExecutionException, TimeoutException {
        final UnitConfig.Builder unitConfigBuilder = Registries.getUnitRegistry().getUnitConfigById(unitId).toBuilder();
        final MetaConfigType.MetaConfig.Builder metaConfigBuilder = unitConfigBuilder.getMetaConfigBuilder();
        for (int i = 0; i < metaConfigBuilder.getEntryCount(); i++) {
            if (metaConfigBuilder.getEntry(i).getKey().equals(entry.getKey())) {
                metaConfigBuilder.removeEntry(i);
                break;
            }
        }
        metaConfigBuilder.addEntry(entry);
        return Registries.getUnitRegistry().updateUnitConfig(unitConfigBuilder.build()).get(5, TimeUnit.SECONDS).getMetaConfig();
    }

//    @Query("unitConfig") todo QueryType required in order to support multible arguments
//    UnitConfig getUnitConfigByAlias(@Arg("alias") String alias) throws CouldNotPerformException, InterruptedException {
//        return Registries.getUnitRegistry(true).getUnitConfigByAlias(alias);
//    }


    // --------------------------------------------------- Stuff that has to be moved to jul --- start ------------------------->

    private static class UnitFilterImpl implements ListFilter<UnitConfig> {

        private final UnitFilter filter;
        private final UnitConfig properties;
        private final Filter andFilter, orFilter;

        private final boolean bypass;

        public UnitFilterImpl(final UnitFilter filter) {
            this.filter = filter;
            this.bypass = !filter.hasProperties();
            this.properties = filter.getProperties();

            if (filter.hasAnd()) {
                this.andFilter = new UnitFilterImpl(filter.getAnd());
            } else {
                this.andFilter = null;
            }

            if (filter.hasOr()) {
                this.orFilter = new UnitFilterImpl(filter.getOr());
            } else {
                this.orFilter = null;
            }
        }

        @Override
        public boolean match(final UnitConfig unitConfig) {
            return (propertyMatch(unitConfig) && andFilterMatch(unitConfig)) || orFilterMatch(unitConfig);
        }

        public boolean propertyMatch(final UnitConfig unitConfig) {

            // handle bypass
            if (bypass) {
                return !filter.getNot();
            }

            // filter by type
            if (properties.hasUnitType() && !(properties.getUnitType().equals(unitConfig.getUnitType()))) {
                return filter.getNot();
            }

            // filter by type
            if (properties.hasUnitType() && !(properties.getUnitType().equals(unitConfig.getUnitType()))) {
                return filter.getNot();
            }

            // filter by location
            if (properties.getPlacementConfig().hasLocationId() && !(properties.getPlacementConfig().getLocationId().equals(unitConfig.getPlacementConfig().getLocationId()))) {
                return filter.getNot();
            }

            // filter by location root
            if (properties.getLocationConfig().hasRoot() && !(properties.getLocationConfig().getRoot() == (unitConfig.getLocationConfig().getRoot()))) {
                return filter.getNot();
            }

            // filter by location type
            if (properties.getLocationConfig().hasLocationType() && !(properties.getLocationConfig().getLocationType() == (unitConfig.getLocationConfig().getLocationType()))) {
                return filter.getNot();
            }

            return !filter.getNot();
        }

        private boolean orFilterMatch(final UnitConfig unitConfig) {
            if (orFilter != null) {
                return orFilter.match(unitConfig);
            }
            return false;
        }

        private boolean andFilterMatch(final UnitConfig unitConfig) {
            if (andFilter != null) {
                return andFilter.match(unitConfig);
            }
            return true;
        }
    }
    // --------------------------------------------------- Stuff that has to be moved to jul --- end ------------------------->
}
