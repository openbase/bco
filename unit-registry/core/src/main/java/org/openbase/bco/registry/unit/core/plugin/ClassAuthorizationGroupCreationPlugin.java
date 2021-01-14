package org.openbase.bco.registry.unit.core.plugin;

/*-
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.registry.clazz.remote.CachedClassRegistryRemote;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MetaConfigPool;
import org.openbase.jul.extension.type.processing.MetaConfigVariableProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.configuration.EntryType.Entry;
import org.openbase.type.language.LabelType.Label;
import org.openbase.type.configuration.MetaConfigType.MetaConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.type.domotic.unit.agent.AgentClassType.AgentClass;
import org.openbase.type.domotic.unit.app.AppClassType.AppClass;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * This plugin makes sure that an authorization group exists for ever agent and app class.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ClassAuthorizationGroupCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassAuthorizationGroupCreationPlugin.class);

    public static final String AGENT_CLASS_ID_KEY = "AGENT_CLASS_ID";
    public static final String APP_CLASS_ID_KEY = "APP_CLASS_ID";

    @Override
    public void init(ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
        super.init(registry);
        // validate on startup
        try {
            beforeUpstreamDependencyNotification(CachedClassRegistryRemote.getRegistry().getAgentClassRemoteRegistry(true));
            beforeUpstreamDependencyNotification(CachedClassRegistryRemote.getRegistry().getAppClassRemoteRegistry(true));
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void beforeUpstreamDependencyNotification(final Registry dependency) throws CouldNotPerformException {
        if (dependency.equals(CachedClassRegistryRemote.getRegistry().getAgentClassRemoteRegistry(true))) {
            final Set<String> authorizationGroupIdSet = new HashSet<>();
            for (final AgentClass agentClass : CachedClassRegistryRemote.getRegistry().getAgentClasses()) {
                try {
                    authorizationGroupIdSet.add(findByAgentClass(agentClass).getId());
                } catch (NotAvailableException ex) {
                    // register a new authorization group
                    authorizationGroupIdSet.add(registerGroup(AGENT_CLASS_ID_KEY, agentClass.getId(), agentClass.getLabel()).getId());
                }
            }
            removeUnneededGroups(authorizationGroupIdSet, AGENT_CLASS_ID_KEY);
        }

        if (dependency.equals(CachedClassRegistryRemote.getRegistry().getAppClassRemoteRegistry(true))) {
            final Set<String> authorizationGroupIdSet = new HashSet<>();
            for (final AppClass appClass : CachedClassRegistryRemote.getRegistry().getAppClasses()) {
                try {
                    authorizationGroupIdSet.add(findByAppClass(appClass).getId());
                } catch (NotAvailableException ex) {
                    // register a new authorization group
                    authorizationGroupIdSet.add(registerGroup(APP_CLASS_ID_KEY, appClass.getId(), appClass.getLabel()).getId());
                }
            }
            removeUnneededGroups(authorizationGroupIdSet, APP_CLASS_ID_KEY);
        }
    }

    private void removeUnneededGroups(final Set<String> verifiedIdSet, final String propertyKey) throws CouldNotPerformException {
        final Set<UnitConfig> authorizationGroupToRemove = new HashSet<>();
        for (final UnitConfig unitConfig : getRegistry().getMessages()) {
            if (verifiedIdSet.contains(unitConfig.getId())) {
                continue;
            }

            final MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider(UnitConfigProcessor.getDefaultAlias(unitConfig, "?") + MetaConfig.class.getSimpleName(),
                    unitConfig.getMetaConfig()));
            try {
                metaConfigPool.getValue(propertyKey);
                authorizationGroupToRemove.add(unitConfig);
            } catch (NotAvailableException ex) {
                // ignore authorization groups where the property is not set
            }
        }

        for (UnitConfig unitConfig : authorizationGroupToRemove) {
            getRegistry().remove(unitConfig);
        }
    }

    private UnitConfig findByAgentClass(final AgentClass agentClass) throws CouldNotPerformException {
        try {
            return findByPropertyAndId(AGENT_CLASS_ID_KEY, agentClass.getId());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Could not find authorizationGroup for agentClass[" + LabelProcessor.getBestMatch(agentClass.getLabel()) + "]");
        }
    }

    private UnitConfig findByAppClass(final AppClass appClass) throws CouldNotPerformException {
        try {
            return findByPropertyAndId(APP_CLASS_ID_KEY, appClass.getId());
        } catch (NotAvailableException ex) {
            throw new NotAvailableException("Could not find authorizationGroup for appClass[" + LabelProcessor.getBestMatch(appClass.getLabel()) + "]");
        }
    }

    private UnitConfig findByPropertyAndId(final String propertyKey, final String id) throws CouldNotPerformException {
        for (final UnitConfig authorizationGroup : getRegistry().getMessages()) {
            MetaConfigPool metaConfigPool = new MetaConfigPool();
            metaConfigPool.register(new MetaConfigVariableProvider(authorizationGroup.getAlias(0) + MetaConfig.class.getSimpleName(),
                    authorizationGroup.getMetaConfig()));
            try {
                final String idInMetaConfig = metaConfigPool.getValue(propertyKey);
                if (idInMetaConfig.equals(id)) {
                    return authorizationGroup;
                }
            } catch (NotAvailableException ex) {
                // ignore authorization groups where the property is not set
            }
        }
        throw new NotAvailableException("User for class[" + id + "]");
    }

    private UnitConfig registerGroup(final String propertyKey, final String id, final Label classLabel) throws CouldNotPerformException {
        final UnitConfig.Builder authGroup = UnitConfig.newBuilder();
        authGroup.setUnitType(UnitType.AUTHORIZATION_GROUP);
        final String label = LabelProcessor.getBestMatch(classLabel) + StringProcessor.transformUpperCaseToPascalCase(UnitType.AUTHORIZATION_GROUP.name());
        LabelProcessor.addLabel(authGroup.getLabelBuilder(), Locale.ENGLISH, label);
        Entry.Builder entry = authGroup.getMetaConfigBuilder().addEntryBuilder();
        entry.setKey(propertyKey).setValue(id);
        return getRegistry().register(authGroup.build());
    }
}
