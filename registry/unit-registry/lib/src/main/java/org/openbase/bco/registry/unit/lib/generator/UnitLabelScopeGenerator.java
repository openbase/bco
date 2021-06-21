package org.openbase.bco.registry.unit.lib.generator;

/*-
 * #%L
 * BCO Registry Unit Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.registry.clazz.lib.ClassRegistry;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.type.communication.ScopeType;
import org.openbase.type.communication.ScopeType.Scope;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;

import java.util.Locale;

public class UnitLabelScopeGenerator implements UnitScopeGenerator {

    @Override
    public Scope generateScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitRegistry == null) {
            throw new NotAvailableException("UnitRegistry");
        }

        if (classRegistry == null) {
            throw new NotAvailableException("ClassRegistry");
        }

        switch (unitConfig.getUnitType()) {
            case LOCATION:
                return generateLocationScope(unitConfig, unitRegistry, classRegistry);
            case DEVICE:
                return generateDeviceScope(unitConfig, unitRegistry, classRegistry);
            case CONNECTION:
                return generateConnectionScope(unitConfig, unitRegistry, classRegistry);
            case UNIT_GROUP:
                return generateUnitGroupScope(unitConfig, unitRegistry, classRegistry);
            case AGENT:
                return generateAgentScope(unitConfig, unitRegistry, classRegistry);
            case APP:
                return generateAppScope(unitConfig, unitRegistry, classRegistry);
            case SCENE:
                return generateSceneScope(unitConfig, unitRegistry, classRegistry);
            case USER:
                return generateUserScope(unitConfig, unitRegistry, classRegistry);
            case AUTHORIZATION_GROUP:
                return generateAuthorizationGroupScope(unitConfig, unitRegistry, classRegistry);
            case GATEWAY:
                return generateGatewayScope(unitConfig, unitRegistry, classRegistry);
            default:
                return generateUnitScope(unitConfig, unitRegistry, classRegistry);
        }
    }

    private static ScopeType.Scope generateLocationScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLocationConfig() || unitConfig.getLocationConfig() == null) {
            throw new NotAvailableException("locationConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("location.label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("location.placementconfig");
        }

        if (!unitConfig.getPlacementConfig().hasLocationId() || unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
            throw new NotAvailableException("location.placementconfig.locationid");
        }

        ScopeType.Scope.Builder scope = ScopeType.Scope.newBuilder();
        if (!unitConfig.getLocationConfig().getRoot()) {
            scope.addAllComponent(unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION).getScope().getComponentList());
        }
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    private static ScopeType.Scope generateConnectionScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        final String defaultLabel = LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel());
        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder locationScope = locationConfig.getScope().toBuilder();

        // add unit type
        locationScope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name().replace("_", "")));

        // add unit label
        locationScope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(defaultLabel));

        return locationScope.build();
    }

    private static ScopeType.Scope generateDeviceScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("device label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder locationScope = locationConfig.getScope().toBuilder();

        // add type 'device'
        locationScope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("device"));

        // add device scope
        locationScope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return locationScope.build();
    }

    private static ScopeType.Scope generateUnitScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name().replace("_", "")));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    private static ScopeType.Scope generateUnitGroupScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("UnitGroup"));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    private static ScopeType.Scope generateAgentScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        final AgentClass agentClass = classRegistry.getAgentClassById(unitConfig.getAgentConfig().getAgentClassId());

        if (!unitConfig.hasAgentConfig() || unitConfig.getAgentConfig() == null) {
            throw new NotAvailableException("unitConfig.agentConfig");
        }

        if (!agentClass.hasLabel()) {
            throw new NotAvailableException("agentClass.label");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("locationUnitConfig.scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name()));

        // add agent class label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, agentClass.getLabel())));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    private static ScopeType.Scope generateAppScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        final AppClass appClass = classRegistry.getAppClassById(unitConfig.getAppConfig().getAppClassId());


        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("appConfig.label");
        }

        if (!appClass.hasLabel()) {
            throw new NotAvailableException("appClass.label");
        }
        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name()));

        // add unit app
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, appClass.getLabel())));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    private static ScopeType.Scope generateSceneScope(final UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("sceneConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("sceneConfig.label");
        }
        
        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name()));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    private static ScopeType.Scope generateUserScope(final UnitConfig userUnitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (userUnitConfig == null) {
            throw new NotAvailableException("userUnitConfig");
        }

        if (!userUnitConfig.hasUserConfig()) {
            throw new NotAvailableException("userConfig");
        }

        if (!userUnitConfig.getUserConfig().hasUserName()) {
            throw new NotAvailableException("userConfig.userName");
        }

        if (userUnitConfig.getUserConfig().getUserName().isEmpty()) {
            throw new NotAvailableException("Field userConfig.userName isEmpty");
        }

        // add manager
        ScopeType.Scope.Builder scope = ScopeType.Scope.newBuilder().addComponent(ScopeProcessor.convertIntoValidScopeComponent("manager"));

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(userUnitConfig.getUnitType().name()));

        // add user name
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(userUnitConfig.getUserConfig().getUserName()));

        return scope.build();
    }

    private static ScopeType.Scope generateAuthorizationGroupScope(final UnitConfig authorizationGroupUniConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws CouldNotPerformException {

        if (authorizationGroupUniConfig == null) {
            throw new NotAvailableException("authorizationGroupConfig");
        }

        if (!authorizationGroupUniConfig.hasLabel()) {
            throw new NotAvailableException("authorizationGroupConfig.label");
        }

        // add manager
        ScopeType.Scope.Builder scope = ScopeType.Scope.newBuilder().addComponent(ScopeProcessor.convertIntoValidScopeComponent("manager"));
        // add user
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("authorization"));
        // add group
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("group"));
        // add user name
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, authorizationGroupUniConfig.getLabel())));

        return scope.build();
    }

    private static Scope generateGatewayScope(UnitConfig unitConfig, final UnitRegistry unitRegistry, final ClassRegistry classRegistry) throws NotAvailableException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unit label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }
        
        final UnitConfig locationConfig = unitRegistry.getUnitConfigByIdAndUnitType(unitConfig.getPlacementConfig().getLocationId(), UnitType.LOCATION);

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add type 'unit'
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("gateway"));

        // add device scope
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }
}
