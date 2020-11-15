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
import com.google.common.util.concurrent.ListenableFuture;
import graphql.schema.DataFetchingEnvironment;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.openbase.bco.api.graphql.BCOGraphQLContext;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.activity.ActivityTemplateType.ActivityTemplate;
import org.openbase.type.domotic.authentication.PermissionConfigType.PermissionConfig;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.agent.AgentConfigType.AgentConfig;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.domotic.unit.app.AppConfigType.AppConfig;
import org.openbase.type.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import org.openbase.type.domotic.unit.device.DeviceClassType.DeviceClass;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.location.TileConfigType.TileConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Bundle all schema modifications to add fields to protobuf types.
 * <p>
 * Note:
 * This is needed because there are issues with replacing fields otherwise.
 * Rejoiner has a method to replace fields, but in this case you have to transform
 * the field value through coercing and then you cannot access the context of the
 * request (token, language, ...). Therefore, the fields have to be removed (see
 * SchemaModificationsRemove) and can then be added again (below). Since there is
 * an exception thrown if an existing field is added, the removal has to be performed
 * first. Thus, removal and adding is split into two different classes and the
 * removal module is registered before the modifications module (see
 * BCOGraphQlApiSpringBootApplication).
 */
public class SchemaModificationsAdd extends SchemaModule {

    private final Logger logger = LoggerFactory.getLogger(SchemaModificationsAdd.class);

    @SchemaModification(addField = "units", onType = LocationConfig.class)
    ListenableFuture<List<UnitConfig>> locationConfigUnits(LocationConfig locationConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(locationConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "children", onType = LocationConfig.class)
    ListenableFuture<List<UnitConfig>> locationConfigChildren(LocationConfig locationConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(locationConfig.getChildIdList()));
    }

    @SchemaModification(addField = "owner", onType = PermissionConfig.class)
    UnitConfig permissionConfigOwner(PermissionConfig permissionConfig, DataFetchingEnvironment env) throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitConfigById(permissionConfig.getOwnerId());
    }

    @SchemaModification(addField = "group", onType = PermissionConfig.MapFieldEntry.class)
    UnitConfig permissionConfigMapFieldEntryGroup(PermissionConfig.MapFieldEntry mapFieldEntry) throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitConfigById(mapFieldEntry.getGroupId());
    }

    @SchemaModification(addField = "activityTemplate", onType = ActivityConfig.class)
    ActivityTemplate activityConfigActivityTemplate(ActivityConfig activityConfig) throws CouldNotPerformException {
        return Registries.getTemplateRegistry().getActivityTemplateById(activityConfig.getActivityTemplateId());
    }

    @SchemaModification(addField = "labelString", onType = UnitConfig.class)
    String addLabelBestMatch(UnitConfig unitConfig, DataFetchingEnvironment env) {
        BCOGraphQLContext context = env.getContext();
        try {
            return LabelProcessor.getBestMatch(context.getLanguageCode(), unitConfig.getLabel());
        } catch (NotAvailableException e) {
            try {
                return LabelProcessor.getFirstLabel(unitConfig.getLabel());
            } catch (NotAvailableException ex) {
                ExceptionPrinter.printHistory(ex, logger);
                return "";
            }
        }
    }

    @SchemaModification(addField = "descriptionString", onType = UnitConfig.class)
    String addDescriptionBestMatch(UnitConfig unitConfig, DataFetchingEnvironment env) {
        BCOGraphQLContext context = env.getContext();
        try {
            return MultiLanguageTextProcessor.getMultiLanguageTextByLanguage(context.getLanguageCode(), unitConfig.getDescription());
        } catch (NotAvailableException e) {
            try {
                return MultiLanguageTextProcessor.getFirstMultiLanguageText(unitConfig.getDescription());
            } catch (NotAvailableException ex) {
                logger.debug("Unit {} does not have a description", unitConfig.getAlias(0));
                return "";
            }
        }
    }

    @SchemaModification(addField = "serviceTemplate", onType = ServiceDescription.class)
    ServiceTemplate serviceDescriptionServiceTemplate(ServiceDescription serviceDescription) throws CouldNotPerformException {
        return Registries.getTemplateRegistry().getServiceTemplateById(serviceDescription.getServiceTemplateId());
    }

    @SchemaModification(addField = "unit", onType = ServiceConfig.class)
    UnitConfig serviceConfigUnitConfig(ServiceConfig serviceConfig) throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitConfigById(serviceConfig.getUnitId());
    }

    @SchemaModification(addField = "agentClass", onType = AgentConfig.class)
    AgentClass agentConfigAgentClass(AgentConfig agentConfig) throws CouldNotPerformException {
        return Registries.getClassRegistry().getAgentClassById(agentConfig.getAgentClassId());
    }

    @SchemaModification(addField = "member", onType = UnitGroupConfig.class)
    ListenableFuture<List<UnitConfig>> unitGroupConfigUnitConfig(UnitGroupConfig unitGroupConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(unitGroupConfig.getMemberIdList()));
    }

    @SchemaModification(addField = "unitTemplate", onType = UnitConfig.class)
    UnitTemplate unitConfigUnitTemplate(UnitConfig unitConfig) throws CouldNotPerformException {
        return Registries.getTemplateRegistry().getUnitTemplateById(unitConfig.getUnitTemplateConfigId());
    }

    @SchemaModification(addField = "unitHost", onType = UnitConfig.class)
    UnitConfig unitConfigUnitHost(UnitConfig unitConfig) throws CouldNotPerformException {
        return Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
    }

    @SchemaModification(addField = "member", onType = AuthorizationGroupConfig.class)
    ListenableFuture<List<UnitConfig>> authorizationGroupConfigUnitConfig(AuthorizationGroupConfig authorizationGroupConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(authorizationGroupConfig.getMemberIdList()));
    }

    @SchemaModification(addField = "connections", onType = TileConfig.class)
    ListenableFuture<List<UnitConfig>> unitConfigUnitHost(TileConfig tileConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(tileConfig.getConnectionIdList()));
    }

    @SchemaModification(addField = "appClass", onType = AppConfig.class)
    AppClass appConfigAppClass(AppConfig appConfig) throws CouldNotPerformException {
        return Registries.getClassRegistry().getAppClassById(appConfig.getAppClassId());
    }

    @SchemaModification(addField = "units", onType = AppConfig.class)
    ListenableFuture<List<UnitConfig>> appConfigUnits(AppConfig appConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(appConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "tiles", onType = ConnectionConfig.class)
    ListenableFuture<List<UnitConfig>> connectionConfigTiles(ConnectionConfig connectionConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(connectionConfig.getTileIdList()));
    }

    @SchemaModification(addField = "units", onType = ConnectionConfig.class)
    ListenableFuture<List<UnitConfig>> connectionConfigUnits(ConnectionConfig connectionConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(connectionConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "deviceClass", onType = DeviceConfig.class)
    DeviceClass deviceConfigDeviceClass(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return Registries.getClassRegistry().getDeviceClassById(deviceConfig.getDeviceClassId());
    }

    @SchemaModification(addField = "units", onType = DeviceConfig.class)
    ListenableFuture<List<UnitConfig>> deviceConfigUnits(DeviceConfig deviceConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader("units").loadMany(deviceConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "location", onType = PlacementConfig.class)
    UnitConfig placementConfigLocation(PlacementConfig placementConfig) throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitConfigById(placementConfig.getLocationId());
    }
}
