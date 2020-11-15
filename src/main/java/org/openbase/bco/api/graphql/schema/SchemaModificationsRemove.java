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
import org.openbase.type.domotic.activity.ActivityConfigType.ActivityConfig;
import org.openbase.type.domotic.authentication.PermissionConfigType;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.agent.AgentConfigType.AgentConfig;
import org.openbase.type.domotic.unit.app.AppConfigType.AppConfig;
import org.openbase.type.domotic.unit.authorizationgroup.AuthorizationGroupConfigType.AuthorizationGroupConfig;
import org.openbase.type.domotic.unit.connection.ConnectionConfigType.ConnectionConfig;
import org.openbase.type.domotic.unit.device.DeviceConfigType.DeviceConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType;
import org.openbase.type.domotic.unit.location.TileConfigType.TileConfig;
import org.openbase.type.domotic.unit.unitgroup.UnitGroupConfigType.UnitGroupConfig;
import org.openbase.type.spatial.PlacementConfigType.PlacementConfig;

/**
 * Bundle all schema modifications to remove fields from protobuf types.
 * <p>
 * Note:
 * This is needed because there are issues with replacing fields otherwise.
 * Rejoiner has a method to replace fields, but in this case you have to transform
 * the field value through coercing and then you cannot access the context of the
 * request (token, language, ...). Therefore, the fields have to be removed (below)
 * and can then be added again (see SchemaModificationsAdd). Since there is
 * an exception thrown if an existing field is added, the removal has to be performed
 * first. Thus, removal and adding is split into two different classes and the
 * removal module is registered before the modifications module (see
 * BCOGraphQlApiSpringBootApplication).
 */
public class SchemaModificationsRemove extends SchemaModule {

    @SchemaModification
    TypeModification removeMultiLanguageFields = Type.find(UnitConfig.getDescriptor()).removeFields("description", "label");

    @SchemaModification
    TypeModification removeLocationConfigUnitId = Type.find(LocationConfigType.LocationConfig.getDescriptor()).removeField("unitId");

    @SchemaModification
    TypeModification removeLocationConfigChildId = Type.find(LocationConfigType.LocationConfig.getDescriptor()).removeField("childId");

    @SchemaModification
    TypeModification removePermissionConfigId = Type.find(PermissionConfigType.PermissionConfig.getDescriptor()).removeField("ownerId");

    @SchemaModification
    TypeModification removePermissionMapFieldEntryId = Type.find(PermissionConfigType.PermissionConfig.MapFieldEntry.getDescriptor()).removeField("groupId");

    @SchemaModification
    TypeModification removeActivityConfigActivityTemplateId = Type.find(ActivityConfig.getDescriptor()).removeField("activityTemplateId");

    @SchemaModification
    TypeModification removeServiceDescriptionServiceTemplateId = Type.find(ServiceDescription.getDescriptor()).removeField("serviceTemplateId");

    @SchemaModification
    TypeModification removeServiceConfigUnitId = Type.find(ServiceConfig.getDescriptor()).removeField("unitId");

    @SchemaModification
    TypeModification removeAgentConfigAgentClassId = Type.find(AgentConfig.getDescriptor()).removeField("agentClassId");

    @SchemaModification
    TypeModification removeUnitGroupConfigMemberId = Type.find(UnitGroupConfig.getDescriptor()).removeField("memberId");

    @SchemaModification
    TypeModification removeUnitConfigUnitTemplateId = Type.find(UnitConfig.getDescriptor()).removeField("unitTemplateId");

    @SchemaModification
    TypeModification removeUnitConfigUnitHostId = Type.find(UnitConfig.getDescriptor()).removeField("unitHostId");

    @SchemaModification
    TypeModification removeAuthorizationGroupMemberId = Type.find(AuthorizationGroupConfig.getDescriptor()).removeField("memberId");

    @SchemaModification
    TypeModification removeTileConfigConnectionId = Type.find(TileConfig.getDescriptor()).removeField("connectionId");

    @SchemaModification
    TypeModification removeAppConfigAppClassId = Type.find(AppConfig.getDescriptor()).removeField("appClassId");

    @SchemaModification
    TypeModification removeAppConfigUnitId = Type.find(AppConfig.getDescriptor()).removeField("unitId");

    @SchemaModification
    TypeModification removeConnectionConfigTileId = Type.find(ConnectionConfig.getDescriptor()).removeField("tileId");

    @SchemaModification
    TypeModification removeConnectionConfigUnitId = Type.find(ConnectionConfig.getDescriptor()).removeField("unitId");

    @SchemaModification
    TypeModification deviceConfigDeviceClassId = Type.find(DeviceConfig.getDescriptor()).removeField("deviceClassId");

    @SchemaModification
    TypeModification deviceConfigUnitId = Type.find(DeviceConfig.getDescriptor()).removeField("unitId");

    @SchemaModification
    TypeModification placementConfigLocationId = Type.find(PlacementConfig.getDescriptor()).removeField("locationId");
}
