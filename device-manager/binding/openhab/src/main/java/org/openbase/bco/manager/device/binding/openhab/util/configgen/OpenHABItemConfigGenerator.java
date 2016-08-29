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
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AbstractItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AgentItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AgentItemEntry.AGENT_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry.APP_GROUP_LABEL;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry.LOCATION_GROUP_LABEL;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry.SCENE_GROUP_LABEL;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.EnablingStateType;
import rst.homeautomation.state.InventoryStateType.InventoryState;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class OpenHABItemConfigGenerator {

    public static final int TAB_SIZE = 4;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABItemConfigGenerator.class);

    private final List<AbstractItemEntry> itemEntryList;
    private final List<GroupEntry> groupEntryList;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationRegistryRemote locationRegistryRemote;
    private final SceneRegistryRemote sceneRegistryRemote;
    private final AgentRegistryRemote agentRegistryRemote;
    private final AppRegistryRemote appRegistryRemote;

    public OpenHABItemConfigGenerator(final DeviceRegistryRemote deviceRegistryRemote, final LocationRegistryRemote locationRegistryRemote, final SceneRegistryRemote sceneRegistryRemote, final AgentRegistryRemote agentRegistryRemote, final AppRegistryRemote appRegistryRemote) throws InstantiationException {
        try {
            this.itemEntryList = new ArrayList<>();
            this.groupEntryList = new ArrayList<>();
            this.deviceRegistryRemote = deviceRegistryRemote;
            this.locationRegistryRemote = locationRegistryRemote;
            this.sceneRegistryRemote = sceneRegistryRemote;
            this.agentRegistryRemote = agentRegistryRemote;
            this.appRegistryRemote = appRegistryRemote;
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException, CouldNotPerformException {
    }

    public synchronized void generate() throws CouldNotPerformException {
        logger.info("generate item config");
        try {
            itemEntryList.clear();
            groupEntryList.clear();
            AbstractItemEntry.reset();
            GroupEntry.reset();
            generateGroupEntries();
            generateItemEntries();
            serializeToFile();
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate item config", ex);
        }
    }

    private void generateGroupEntries() throws CouldNotPerformException {
        try {
            // generate location groups
            GroupEntry groupEntry, rootEntry = null;
            List<LocationConfig> locationConfigList = locationRegistryRemote.getData().getLocationConfigList();
            for (LocationConfig locationConfig : locationConfigList) {
                groupEntry = new GroupEntry(locationConfig, locationRegistryRemote);
                groupEntryList.add(new GroupEntry(locationConfig, locationRegistryRemote));

                if (locationConfig.getRoot()) {
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

    private void generateItemEntries() throws CouldNotPerformException {
        try {
            List<DeviceConfig> deviceConfigList = deviceRegistryRemote.getData().getDeviceConfigList();

            for (DeviceConfig deviceConfig : deviceConfigList) {

                // load device class
                DeviceClass deviceClass = deviceRegistryRemote.getDeviceClassById(deviceConfig.getDeviceClassId());

                // ignore non openhab items
                if (deviceClass.getBindingConfig().getType() != BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB) {
                    continue;
                }

                // ignore non installed items
                if (deviceConfig.getInventoryState().getValue() != InventoryState.State.INSTALLED) {
                    continue;
                }

                for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
                    for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        try {
                            itemEntryList.add(new ServiceItemEntry(deviceClass, deviceConfig, unitConfig, serviceConfig, locationRegistryRemote));
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not generate item for Service[" + serviceConfig.getType().name() + "] of Unit[" + unitConfig.getId() + "]", ex), logger, LogLevel.ERROR);
                        }
                    }
                }
            }

            for (LocationConfig locationConfig : locationRegistryRemote.getLocationConfigs()) {
                List<ServiceType> serviceTypesOnLocation = new ArrayList<>();
                for (UnitConfig unitConfig : deviceRegistryRemote.getUnitConfigs()) {
                    if (locationConfig.getUnitIdList().contains(unitConfig.getId())) {
                        for (ServiceType serviceType : deviceRegistryRemote.getUnitTemplateByType(unitConfig.getType()).getServiceTypeList()) {
                            if (!serviceTypesOnLocation.contains(serviceType)) {
                                serviceTypesOnLocation.add(serviceType);
                            }
                        }
                    }
                }
                for (ServiceType serviceType : serviceTypesOnLocation) {
                    if (serviceType == ServiceType.COLOR_SERVICE || serviceType == ServiceType.POWER_SERVICE || serviceType == ServiceType.POWER_CONSUMPTION_PROVIDER) {
                        LocationItemEntry entry = new LocationItemEntry(locationConfig, serviceType);
                        itemEntryList.add(entry);
                        logger.info("Added location entry [" + entry.buildStringRep() + "]");
                    }
                }
            }

            for (SceneConfig sceneConfig : sceneRegistryRemote.getSceneConfigs()) {
                // Skip disabled scenes
                if (sceneConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new SceneItemEntry(sceneConfig, locationRegistryRemote));
                }
            }

            for (AgentConfig agentConfig : agentRegistryRemote.getAgentConfigs()) {
                // Skip disabled agents
                if (agentConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new AgentItemEntry(agentConfig, locationRegistryRemote));
                }
            }

            for (AppConfig appConfig : appRegistryRemote.getAppConfigs()) {
                // Skip disabled apps
                if (appConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new AppItemEntry(appConfig, locationRegistryRemote));
                }
            }

            // sort items by command type and label
            Collections.sort(itemEntryList, new Comparator<AbstractItemEntry>() {

                @Override
                public int compare(AbstractItemEntry o1, AbstractItemEntry o2) {
                    int typeComparation = o1.getCommandType().compareTo(o2.getCommandType());
                    if (typeComparation != 0) {
                        return typeComparation;
                    } else {
                        return o1.getLabel().compareTo(o2.getLabel());
                    }
                }
            });

        } catch (Exception ex) {
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
            FileUtils.writeStringToFile(configFile, configAsString, Charset.forName("UTF8") , false);

            logger.info("ItemConfig[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize itemconfig to file!", ex);
        }
    }

    public void shutdown() {

    }
}
