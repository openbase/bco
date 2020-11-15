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
import com.google.protobuf.Descriptors;
import org.openbase.jul.processing.StringProcessor;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private String fieldNameByNumber(final int fieldNumber, final Descriptors.Descriptor descriptor) {
        return StringProcessor.transformToCamelCase(descriptor.findFieldByNumber(fieldNumber).getName());
    }

    private TypeModification removeFieldByNumber(final int fieldNumber, final Descriptors.Descriptor descriptor) {
        return Type.find(descriptor).removeField(fieldNameByNumber(fieldNumber, descriptor));
    }

    @SchemaModification
    TypeModification removeMultiLanguageFields = Type.find(UnitConfig.getDescriptor()).removeFields(
            fieldNameByNumber(UnitConfig.DESCRIPTION_FIELD_NUMBER, UnitConfig.getDescriptor()),
            fieldNameByNumber(UnitConfig.LABEL_FIELD_NUMBER, UnitConfig.getDescriptor()));

    @SchemaModification
    TypeModification removeLocationConfigUnitId = removeFieldByNumber(LocationConfigType.LocationConfig.UNIT_ID_FIELD_NUMBER, LocationConfigType.LocationConfig.getDescriptor());

    @SchemaModification
    TypeModification removeLocationConfigChildId = removeFieldByNumber(LocationConfigType.LocationConfig.CHILD_ID_FIELD_NUMBER, LocationConfigType.LocationConfig.getDescriptor());

    @SchemaModification
    TypeModification removePermissionConfigOwnerId = removeFieldByNumber(PermissionConfigType.PermissionConfig.OWNER_ID_FIELD_NUMBER, PermissionConfigType.PermissionConfig.getDescriptor());

    @SchemaModification
    TypeModification removePermissionMapFieldEntryId = removeFieldByNumber(PermissionConfigType.PermissionConfig.MapFieldEntry.GROUP_ID_FIELD_NUMBER, PermissionConfigType.PermissionConfig.MapFieldEntry.getDescriptor());

    @SchemaModification
    TypeModification removeActivityConfigActivityTemplateId = removeFieldByNumber(ActivityConfig.ACTIVITY_TEMPLATE_ID_FIELD_NUMBER, ActivityConfig.getDescriptor());

    @SchemaModification
    TypeModification removeServiceDescriptionServiceTemplateId = removeFieldByNumber(ServiceDescription.SERVICE_TEMPLATE_ID_FIELD_NUMBER, ServiceDescription.getDescriptor());

    @SchemaModification
    TypeModification removeServiceConfigUnitId = removeFieldByNumber(ServiceConfig.UNIT_ID_FIELD_NUMBER, ServiceConfig.getDescriptor());

    @SchemaModification
    TypeModification removeAgentConfigAgentClassId = removeFieldByNumber(AgentConfig.AGENT_CLASS_ID_FIELD_NUMBER, AgentConfig.getDescriptor());

    @SchemaModification
    TypeModification removeUnitGroupConfigMemberId = removeFieldByNumber(UnitGroupConfig.MEMBER_ID_FIELD_NUMBER, UnitGroupConfig.getDescriptor());

    @SchemaModification
    TypeModification removeUnitConfigUnitTemplateId = removeFieldByNumber(UnitConfig.UNIT_TEMPLATE_CONFIG_ID_FIELD_NUMBER, UnitConfig.getDescriptor());

    @SchemaModification
    TypeModification removeUnitConfigUnitHostId = removeFieldByNumber(UnitConfig.UNIT_HOST_ID_FIELD_NUMBER, UnitConfig.getDescriptor());

    @SchemaModification
    TypeModification removeAuthorizationGroupMemberId = removeFieldByNumber(AuthorizationGroupConfig.MEMBER_ID_FIELD_NUMBER, AuthorizationGroupConfig.getDescriptor());

    @SchemaModification
    TypeModification removeTileConfigConnectionId = removeFieldByNumber(TileConfig.CONNECTION_ID_FIELD_NUMBER, TileConfig.getDescriptor());

    @SchemaModification
    TypeModification removeAppConfigAppClassId = removeFieldByNumber(AppConfig.APP_CLASS_ID_FIELD_NUMBER, AppConfig.getDescriptor());

    @SchemaModification
    TypeModification removeAppConfigUnitId = removeFieldByNumber(AppConfig.UNIT_ID_FIELD_NUMBER, AppConfig.getDescriptor());

    @SchemaModification
    TypeModification removeConnectionConfigTileId = removeFieldByNumber(ConnectionConfig.TILE_ID_FIELD_NUMBER, ConnectionConfig.getDescriptor());

    @SchemaModification
    TypeModification removeConnectionConfigUnitId = removeFieldByNumber(ConnectionConfig.UNIT_ID_FIELD_NUMBER, ConnectionConfig.getDescriptor());

    @SchemaModification
    TypeModification deviceConfigDeviceClassId = removeFieldByNumber(DeviceConfig.DEVICE_CLASS_ID_FIELD_NUMBER, DeviceConfig.getDescriptor());

    @SchemaModification
    TypeModification deviceConfigUnitId = removeFieldByNumber(DeviceConfig.UNIT_ID_FIELD_NUMBER, DeviceConfig.getDescriptor());

    @SchemaModification
    TypeModification placementConfigLocationId = removeFieldByNumber(PlacementConfig.LOCATION_ID_FIELD_NUMBER, PlacementConfig.getDescriptor());
}
