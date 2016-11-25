package org.openbase.bco.manager.device.binding.openhab.util.configgen;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 openbase.org
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
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AgentItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AgentItemEntry.AGENT_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry.APP_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry.LOCATION_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry.SCENE_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.bco.registry.device.lib.DeviceRegistry;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.openbase.bco.registry.unit.lib.UnitRegistry;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType;
import rst.domotic.state.InventoryStateType.InventoryState;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.domotic.unit.device.DeviceClassType.DeviceClass;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABItemConfigGenerator {

    public static final int TAB_SIZE = 4;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABItemConfigGenerator.class);

    private final List<AbstractItemEntry> itemEntryList;
    private final List<GroupEntry> groupEntryList;
    private final UnitRegistry unitRegistry;
    private final LocationRegistryRemote locationRegistryRemote;
    private final SceneRegistryRemote sceneRegistryRemote;
    private final AgentRegistryRemote agentRegistryRemote;
    private final AppRegistryRemote appRegistryRemote;
    private final DeviceRegistry deviceRegistry;

    public OpenHABItemConfigGenerator(final DeviceRegistry deviceRegistry, final UnitRegistry unitRegistry, final LocationRegistryRemote locationRegistryRemote, final SceneRegistryRemote sceneRegistryRemote, final AgentRegistryRemote agentRegistryRemote, final AppRegistryRemote appRegistryRemote) throws InstantiationException {
        try {
            this.itemEntryList = new ArrayList<>();
            this.groupEntryList = new ArrayList<>();
            this.unitRegistry = unitRegistry;
            this.deviceRegistry = deviceRegistry;
            this.locationRegistryRemote = locationRegistryRemote;
            this.sceneRegistryRemote = sceneRegistryRemote;
            this.agentRegistryRemote = agentRegistryRemote;
            this.appRegistryRemote = appRegistryRemote;
        } catch (NullPointerException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException, CouldNotPerformException {
    }

    public synchronized void generate() throws CouldNotPerformException, InterruptedException {
        logger.info("generate item config");
        try {
            itemEntryList.clear();
            groupEntryList.clear();
            AbstractItemEntry.reset();
            GroupEntry.reset();
            generateGroupEntries();
            generateItemEntries();
            serializeToFile();
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Could not generate item config", ex);
        }
    }

    private void generateGroupEntries() throws CouldNotPerformException {
        try {
            // generate location groups
            GroupEntry groupEntry, rootEntry = null;
            List<UnitConfig> locationConfigList = locationRegistryRemote.getData().getLocationUnitConfigList();
            for (final UnitConfig locationUnitConfig : locationConfigList) {
                groupEntry = new GroupEntry(locationUnitConfig, locationRegistryRemote);
                groupEntryList.add(new GroupEntry(locationUnitConfig, locationRegistryRemote));

                if (locationUnitConfig.getLocationConfig().getRoot()) {
                    rootEntry = groupEntry;
                }
            }
            Collections.sort(groupEntryList, (GroupEntry o1, GroupEntry o2) -> o1.getLabel().compareTo(o2.getLabel()));
            generateOverviewGroupEntries(rootEntry);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate group entries.", ex);
        }
    }

    private void generateOverviewGroupEntries(final GroupEntry rootGroupEntry) throws CouldNotPerformException {
        // generate overview menu
        GroupEntry overviewGroupEntry = new GroupEntry("Overview", "Übersicht", "settings", rootGroupEntry);
        groupEntryList.add(overviewGroupEntry);

        for (UnitType unitType : UnitType.values()) {
            if (unitType.equals(UnitType.UNKNOWN)) {
                continue;
            }
            String unitLabel = StringProcessor.transformUpperCaseToCamelCase(unitType.name()) + "Unit";
            groupEntryList.add(new GroupEntry(unitLabel, unitLabel, unitLabel, overviewGroupEntry));
        }

        groupEntryList.add(new GroupEntry(SCENE_GROUP_LABEL, SCENE_GROUP_LABEL, SCENE_GROUP_LABEL, overviewGroupEntry));
        groupEntryList.add(new GroupEntry(AGENT_GROUP_LABEL, AGENT_GROUP_LABEL, AGENT_GROUP_LABEL, overviewGroupEntry));
        groupEntryList.add(new GroupEntry(APP_GROUP_LABEL, APP_GROUP_LABEL, APP_GROUP_LABEL, overviewGroupEntry));
        groupEntryList.add(new GroupEntry(LOCATION_GROUP_LABEL, LOCATION_GROUP_LABEL, LOCATION_GROUP_LABEL, overviewGroupEntry));

//        for (ServiceType serviceType : ServiceType.values()) {
//            groupEntryList.add(new GroupEntry(serviceType.name(), serviceType.name(), "", rootGroupEntry));
//        }
    }

    private void generateItemEntries() throws CouldNotPerformException, InterruptedException {
        try {
            final List<UnitConfig> deviceUnitConfigList = unitRegistry.getUnitConfigs(UnitType.DEVICE);

            for (UnitConfig deviceUnitConfig : deviceUnitConfigList) {

                // load device class
                DeviceClass deviceClass = deviceRegistry.getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());

                // ignore non openhab items
                if (!deviceClass.getBindingConfig().getBindingId().equals("OPENHAB")) {
                    continue;
                }

                // ignore non installed items
                if (deviceUnitConfig.getDeviceConfig().getInventoryState().getValue() != InventoryState.State.INSTALLED) {
                    continue;
                }

                final List<UnitConfig> dalUnitConfigList = new ArrayList<>();

                for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                    dalUnitConfigList.add(unitRegistry.getUnitConfigById(unitId));
                }

                for (final UnitConfig unitConfig : dalUnitConfigList) {

                    // ignore disabled units
                    if (!unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED)) {
                        continue;
                    }

                    Set<ServiceType> serviceTypeSet = new HashSet<>();
                    for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        if (serviceConfig.getServiceTemplate().getPattern() == ServiceTemplate.ServicePattern.CONSUMER) {
                            continue;
                        }

                        if (serviceTypeSet.contains(serviceConfig.getServiceTemplate().getType())) {
                            continue;
                        }

                        if (serviceConfig.getServiceTemplate().getPattern() == ServiceTemplate.ServicePattern.PROVIDER) {
                            if (unitHasServiceAsOperationService(unitConfig, serviceConfig.getServiceTemplate().getType())) {
                                continue;
                            }
                        }

                        serviceTypeSet.add(serviceConfig.getServiceTemplate().getType());
                        try {
                            itemEntryList.add(new ServiceItemEntry(deviceClass, deviceUnitConfig.getMetaConfig(), unitConfig, serviceConfig, locationRegistryRemote));
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not generate item for Service[" + serviceConfig.getServiceTemplate().getType().name() + "] of Unit[" + unitConfig.getId() + "]", ex), logger, LogLevel.ERROR);
                        }
                    }
                }
            }

            for (UnitConfig locationUnitConfig : locationRegistryRemote.getLocationConfigs()) {
                Map<ServiceType, ServiceTemplate> serviceTemplatesOnLocation = new HashMap<>();
                for (UnitConfig unitConfig : unitRegistry.getUnitConfigs()) {
                    if (locationUnitConfig.getLocationConfig().getUnitIdList().contains(unitConfig.getId())) {
                        for (ServiceTemplate serviceTemplate : unitRegistry.getUnitTemplateByType(unitConfig.getType()).getServiceTemplateList()) {
                            if (!serviceTemplatesOnLocation.containsKey(serviceTemplate.getType())) {
                                serviceTemplatesOnLocation.put(serviceTemplate.getType(), serviceTemplate);
                            } else {
                                if (serviceTemplatesOnLocation.get(serviceTemplate.getType()).getPattern() == ServiceTemplate.ServicePattern.PROVIDER
                                        && serviceTemplate.getPattern() == ServiceTemplate.ServicePattern.OPERATION) {
                                    serviceTemplatesOnLocation.put(serviceTemplate.getType(), serviceTemplate);
                                }
                            }
                        }
                    }
                }
                for (ServiceTemplate serviceTemplate : serviceTemplatesOnLocation.values()) {
                    if (serviceTemplate.getType() == ServiceType.COLOR_STATE_SERVICE
                            || serviceTemplate.getType() == ServiceType.POWER_STATE_SERVICE
                            || serviceTemplate.getType() == ServiceType.POWER_CONSUMPTION_STATE_SERVICE) {
                        LocationItemEntry entry = new LocationItemEntry(locationUnitConfig, serviceTemplate);
                        itemEntryList.add(entry);
                        logger.info("Added location entry [" + entry.buildStringRep() + "]");
                    }
                }
            }

            for (final UnitConfig sceneUnitConfig : sceneRegistryRemote.getSceneConfigs()) {
                // Skip disabled scenes
                if (sceneUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new SceneItemEntry(sceneUnitConfig, locationRegistryRemote));
                }
            }

            for (final UnitConfig agentUnitConfig : agentRegistryRemote.getAgentConfigs()) {
                // Skip disabled agents
                if (agentUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new AgentItemEntry(agentUnitConfig, locationRegistryRemote));
                }
            }

            for (UnitConfig appUnitConfig : appRegistryRemote.getAppConfigs()) {
                // Skip disabled apps
                if (appUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new AppItemEntry(appUnitConfig, locationRegistryRemote));
                }
            }

            // sort items by command type and label
            Collections.sort(itemEntryList, (AbstractItemEntry o1, AbstractItemEntry o2) -> {
                int typeComparation = o1.getCommandType().compareTo(o2.getCommandType());
                if (typeComparation != 0) {
                    return typeComparation;
                } else {
                    return o1.getLabel().compareTo(o2.getLabel());
                }
            });

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate item entries.", ex);
        }
    }

    private void serializeToFile() throws CouldNotPerformException {
        try {
            String configAsString = "";

            File configFile = JPService.getProperty(JPOpenHABItemConfig.class).getValue();

            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === BCO AUTO GENERATED GROUP ENTRIES ============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += System.lineSeparator();
            for (GroupEntry entry : groupEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();
            configAsString += System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === BCO AUTO GENERATED ITEM ENTRIES =============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += System.lineSeparator();
            for (AbstractItemEntry entry : itemEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();

            // TODO need to be tested!
            FileUtils.writeStringToFile(configFile, configAsString, Charset.forName("UTF8"), false);

            logger.info("ItemConfig[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize itemconfig to file!", ex);
        }
    }

    public void shutdown() {

    }

    private boolean unitHasServiceAsOperationService(UnitConfig unitConfig, ServiceType serviceType) {
        return unitConfig.getServiceConfigList().stream().anyMatch((tmpServiceConfig) -> (tmpServiceConfig.getServiceTemplate().getType() == serviceType
                && tmpServiceConfig.getServiceTemplate().getPattern() == ServiceTemplate.ServicePattern.OPERATION));
    }
}
