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
import org.openbase.bco.api.graphql.context.AbstractBCOGraphQLContext;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.EnumNotSupportedException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
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
import org.openbase.type.domotic.unit.gateway.GatewayClassType.GatewayClass;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig;
import org.openbase.type.domotic.unit.location.TileConfigType.TileConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.language.LabelType;
import org.openbase.type.language.MultiLanguageTextType;
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
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(locationConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "children", onType = LocationConfig.class)
    ListenableFuture<List<UnitConfig>> locationConfigChildren(LocationConfig locationConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(locationConfig.getChildIdList()));
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
        return getLabelForContext(unitConfig.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "labelString", onType = UnitTemplate.class)
    String addLabelBestMatch(UnitTemplate unitTemplate, DataFetchingEnvironment env) {
        return getLabelForContext(unitTemplate.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "labelString", onType = AgentClass.class)
    String addLabelBestMatch(AgentClass agentClass, DataFetchingEnvironment env) {
        return getLabelForContext(agentClass.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "labelString", onType = AppClass.class)
    String addLabelBestMatch(AppClass appClass, DataFetchingEnvironment env) {
        return getLabelForContext(appClass.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "labelString", onType = DeviceClass.class)
    String addLabelBestMatch(DeviceClass deviceClass, DataFetchingEnvironment env) {
        return getLabelForContext(deviceClass.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "labelString", onType = GatewayClass.class)
    String addLabelBestMatch(GatewayClass gatewayClass, DataFetchingEnvironment env) {
        return getLabelForContext(gatewayClass.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "labelString", onType = ServiceTemplate.class)
    String addLabelBestMatch(ServiceTemplate serviceTemplate, DataFetchingEnvironment env) {
        return getLabelForContext(serviceTemplate.getLabel(), env.getContext());
    }

    @SchemaModification(addField = "descriptionString", onType = UnitConfig.class)
    String addDescriptionBestMatch(UnitConfig unitConfig, DataFetchingEnvironment env) {
        return getTextForContext(unitConfig.getDescription(), env.getContext());
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
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(unitGroupConfig.getMemberIdList()));
    }

    @SchemaModification(addField = "unitTemplate", onType = UnitConfig.class)
    UnitTemplate unitConfigUnitTemplate(UnitConfig unitConfig) throws CouldNotPerformException {
        return Registries.getTemplateRegistry().getUnitTemplateByType(unitConfig.getUnitType());
    }

    @SchemaModification(addField = "unitHost", onType = UnitConfig.class)
    UnitConfig unitConfigUnitHost(UnitConfig unitConfig) throws CouldNotPerformException {
        return Registries.getUnitRegistry().getUnitConfigById(unitConfig.getUnitHostId());
    }

    @SchemaModification(addField = "member", onType = AuthorizationGroupConfig.class)
    ListenableFuture<List<UnitConfig>> authorizationGroupConfigUnitConfig(AuthorizationGroupConfig authorizationGroupConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(authorizationGroupConfig.getMemberIdList()));
    }

    @SchemaModification(addField = "connections", onType = TileConfig.class)
    ListenableFuture<List<UnitConfig>> unitConfigUnitHost(TileConfig tileConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(tileConfig.getConnectionIdList()));
    }

    @SchemaModification(addField = "appClass", onType = AppConfig.class)
    AppClass appConfigAppClass(AppConfig appConfig) throws CouldNotPerformException {
        return Registries.getClassRegistry().getAppClassById(appConfig.getAppClassId());
    }

    @SchemaModification(addField = "units", onType = AppConfig.class)
    ListenableFuture<List<UnitConfig>> appConfigUnits(AppConfig appConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(appConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "tiles", onType = ConnectionConfig.class)
    ListenableFuture<List<UnitConfig>> connectionConfigTiles(ConnectionConfig connectionConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(connectionConfig.getTileIdList()));
    }

    @SchemaModification(addField = "units", onType = ConnectionConfig.class)
    ListenableFuture<List<UnitConfig>> connectionConfigUnits(ConnectionConfig connectionConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(connectionConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "deviceClass", onType = DeviceConfig.class)
    DeviceClass deviceConfigDeviceClass(DeviceConfig deviceConfig) throws CouldNotPerformException {
        return Registries.getClassRegistry().getDeviceClassById(deviceConfig.getDeviceClassId());
    }

    @SchemaModification(addField = "units", onType = DeviceConfig.class)
    ListenableFuture<List<UnitConfig>> deviceConfigUnits(DeviceConfig deviceConfig, DataFetchingEnvironment environment) {
        return FutureConverter.toListenableFuture(environment.<String, UnitConfig>getDataLoader(AbstractBCOGraphQLContext.DATA_LOADER_UNITS).loadMany(deviceConfig.getUnitIdList()));
    }

    @SchemaModification(addField = "location", onType = PlacementConfig.class)
    UnitConfig placementConfigLocation(PlacementConfig placementConfig) throws NotAvailableException {
        return Registries.getUnitRegistry().getUnitConfigById(placementConfig.getLocationId());
    }

    @SchemaModification(addField = "parentTile", onType = PlacementConfig.class)
    UnitConfig placementParentTileConfig(PlacementConfig placementConfig) throws NotAvailableException {
        try {
            return resolveParentTile(placementConfig.getLocationId());

        } catch (NotAvailableException ex) {
            return UnitConfig.getDefaultInstance();
        }
    }

    private UnitConfig resolveParentTile(String locationId) throws NotAvailableException {
        try {
            final UnitConfig locationUnitConfig = Registries.getUnitRegistry().getUnitConfigById(locationId);
            switch (locationUnitConfig.getLocationConfig().getLocationType()) {
                case TILE:
                    return locationUnitConfig;
                case ZONE:
                    throw new InvalidStateException("Zones do not provide a tile!");
                case REGION:
                    return resolveParentTile(locationUnitConfig.getPlacementConfig().getLocationId());
                case UNKNOWN:
                default:
                    throw new EnumNotSupportedException(locationUnitConfig.getLocationConfig().getLocationType(), this);
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Tile");
        }
    }

    @SchemaModification(addField = "descriptionString", onType = GatewayClass.class)
    String addDescriptionBestMatch(GatewayClass gatewayClass, DataFetchingEnvironment env) {
        return getTextForContext(gatewayClass.getDescription(), env.getContext());
    }

    private String getLabelForContext(LabelType.Label label, AbstractBCOGraphQLContext context) {
        return LabelProcessor.getBestMatch(context.getLanguageCode(), label, "?");
    }

    private String getTextForContext(MultiLanguageTextType.MultiLanguageText multiLanguageText, AbstractBCOGraphQLContext context) {
        try {
            return MultiLanguageTextProcessor.getMultiLanguageTextByLanguage(context.getLanguageCode(), multiLanguageText);
        } catch (NotAvailableException e) {
            try {
                return MultiLanguageTextProcessor.getFirstMultiLanguageText(multiLanguageText);
            } catch (NotAvailableException ex) {
                logger.debug("No multi language text available!");
                return "";
            }
        }
    }
}
