package org.openbase.bco.registry.lib.generator;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.ScopeProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.communication.ScopeType.Scope;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;
import org.openbase.type.communication.ScopeType;

import java.util.Collection;
import java.util.Locale;

/**
 * * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class ScopeGenerator {

    public static ScopeType.Scope generateLocationScope(final UnitConfig unitConfig, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLocationConfig() || unitConfig.getLocationConfig() == null) {
            throw new NotAvailableException("locationConfig");
        }

        if (registry == null) {
            throw new NotAvailableException("registry");
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
            scope.addAllComponent(registry.get(unitConfig.getPlacementConfig().getLocationId()).getMessage().getScope().getComponentList());
        }
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel())));

        return scope.build();
    }

    public static ScopeType.Scope generateConnectionScope(final UnitConfig unitConfig, final UnitConfig locationConfig) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        final String defaultLabel = LabelProcessor.getBestMatch(Locale.ENGLISH, unitConfig.getLabel());

        if (locationConfig == null) {
            throw new NotAvailableException("location");
        }

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(unitConfig.getUnitType().name().replace("_", "")));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(defaultLabel));

        return scope.build();
    }

    public static ScopeType.Scope generateDeviceScope(final UnitConfig deviceConfig, final UnitConfig locationConfig) throws CouldNotPerformException {

        if (deviceConfig == null) {
            throw new NotAvailableException("deviceConfig");
        }

        if (!deviceConfig.hasLabel()) {
            throw new NotAvailableException("device label");
        }

        if (!deviceConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        if (locationConfig == null) {
            throw new NotAvailableException("location");
        }

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add type 'device'
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("device"));

        // add device scope
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, deviceConfig.getLabel())));

        return scope.build();
    }

    public static ScopeType.Scope generateUnitScope(final UnitConfig unitConfig, final UnitConfig locationConfig) throws CouldNotPerformException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        if (locationConfig == null) {
            throw new NotAvailableException("location");
        }

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

    public static ScopeType.Scope generateUnitGroupScope(final UnitConfig unitGroupConfig, final UnitConfig locationConfig) throws CouldNotPerformException {

        if (unitGroupConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitGroupConfig.hasLabel()) {
            throw new NotAvailableException("unitConfig.label");
        }

        if (!unitGroupConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        if (locationConfig == null) {
            throw new NotAvailableException("location");
        }

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent("UnitGroup"));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, unitGroupConfig.getLabel())));

        return scope.build();
    }

    public static ScopeType.Scope generateAgentScope(final UnitConfig agentUnitConfig, final AgentClass agentClass, final UnitConfig locationUnitConfig) throws CouldNotPerformException {

        if (agentUnitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (agentClass == null) {
            throw new NotAvailableException("agentClass");
        }

        if (!agentUnitConfig.hasAgentConfig() || agentUnitConfig.getAgentConfig() == null) {
            throw new NotAvailableException("unitConfig.agentConfig");
        }

        if (!agentClass.hasLabel()) {
            throw new NotAvailableException("agentClass.label");
        }

        if (!agentUnitConfig.hasLabel()) {
            throw new NotAvailableException("agentUnitConfig.label");
        }

        if (locationUnitConfig == null) {
            throw new NotAvailableException("location");
        }

        if (!locationUnitConfig.hasScope() || locationUnitConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("locationUnitConfig.scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationUnitConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(agentUnitConfig.getUnitType().name()));

        // add agent class label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, agentClass.getLabel())));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, agentUnitConfig.getLabel())));

        return scope.build();
    }

    public static ScopeType.Scope generateAppScope(final UnitConfig appUnitConfig, final AppClass appClass, final UnitConfig locationUnitConfig) throws CouldNotPerformException {

        if (appUnitConfig == null) {
            throw new NotAvailableException("appConfig");
        }

        if (appClass == null) {
            throw new NotAvailableException("appClass");
        }

        if (!appUnitConfig.hasLabel()) {
            throw new NotAvailableException("appConfig.label");
        }

        if (!appClass.hasLabel()) {
            throw new NotAvailableException("appClass.label");
        }

        if (locationUnitConfig == null) {
            throw new NotAvailableException("location");
        }

        if (!locationUnitConfig.hasScope() || locationUnitConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationUnitConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(appUnitConfig.getUnitType().name()));

        // add unit app
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, appClass.getLabel())));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, appUnitConfig.getLabel())));

        return scope.build();
    }

    public static ScopeType.Scope generateSceneScope(final UnitConfig sceneUnitConfig, final UnitConfig locationConfig) throws CouldNotPerformException {

        if (sceneUnitConfig == null) {
            throw new NotAvailableException("sceneConfig");
        }

        if (!sceneUnitConfig.hasLabel()) {
            throw new NotAvailableException("sceneConfig.label");
        }

        if (locationConfig == null) {
            throw new NotAvailableException("location");
        }

        if (!locationConfig.hasScope() || locationConfig.getScope().getComponentList().isEmpty()) {
            throw new NotAvailableException("location scope");
        }

        // add location scope
        ScopeType.Scope.Builder scope = locationConfig.getScope().toBuilder();

        // add unit type
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(sceneUnitConfig.getUnitType().name()));

        // add unit label
        scope.addComponent(ScopeProcessor.convertIntoValidScopeComponent(LabelProcessor.getBestMatch(Locale.ENGLISH, sceneUnitConfig.getLabel())));

        return scope.build();
    }

    public static ScopeType.Scope generateUserScope(final UnitConfig userUnitConfig) throws CouldNotPerformException {

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

    public static ScopeType.Scope generateAuthorizationGroupScope(final UnitConfig authorizationGroupUniConfig) throws CouldNotPerformException {

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

    public static Scope generateGatewayScope(UnitConfig unitConfig, UnitConfig locationConfig) throws NotAvailableException {

        if (unitConfig == null) {
            throw new NotAvailableException("unitConfig");
        }

        if (!unitConfig.hasLabel()) {
            throw new NotAvailableException("unit label");
        }

        if (!unitConfig.hasPlacementConfig()) {
            throw new NotAvailableException("placement config");
        }

        if (locationConfig == null) {
            throw new NotAvailableException("location");
        }

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
