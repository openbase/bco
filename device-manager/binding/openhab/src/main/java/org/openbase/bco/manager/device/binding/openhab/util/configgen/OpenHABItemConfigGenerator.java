package org.openbase.bco.manager.device.binding.openhab.util.configgen;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.AppItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ItemIdGenerator;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.LocationItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.SceneItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.UnitGroupItemEntry;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceDescriptionType.ServiceDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.EnablingStateType;
import rst.domotic.state.EnablingStateType.EnablingState;
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

    public OpenHABItemConfigGenerator() throws InstantiationException {
        try {
            this.itemEntryList = new ArrayList<>();
            this.groupEntryList = new ArrayList<>();
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
            for (final UnitConfig locationUnitConfig : Registries.getUnitRegistry(true).getUnitConfigs(UnitType.LOCATION)) {
                groupEntry = new GroupEntry(locationUnitConfig);
                groupEntryList.add(new GroupEntry(locationUnitConfig));

                if (locationUnitConfig.getLocationConfig().getRoot()) {
                    rootEntry = groupEntry;
                }
            }

            if (rootEntry == null) {
                logger.warn("Group entries could not be generated because the root location is still missing! Register at least one location if the group items should be generated.");
                return;
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
            groupEntryList.add(new GroupEntry(ItemIdGenerator.generateUnitGroupID(unitType), StringProcessor.transformUpperCaseToCamelCase(unitType.name()), unitType.name(), overviewGroupEntry));
        }

        for (ServiceType serviceType : ServiceType.values()) {
            groupEntryList.add(new GroupEntry(ItemIdGenerator.generateServiceGroupID(serviceType), StringProcessor.transformUpperCaseToCamelCase(serviceType.name()), serviceType.name(), overviewGroupEntry));
        }
    }

    private synchronized void generateItemEntries() throws CouldNotPerformException, InterruptedException {
        try {
            for (UnitConfig locationUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.LOCATION)) {
                Map<ServiceType, ServiceDescription> serviceDescriptionsOnLocation = new HashMap<>();
                for (final String childUnitId : locationUnitConfig.getLocationConfig().getChildIdList()) {
                    final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(childUnitId);
                    for (ServiceDescription serviceDescription : Registries.getUnitRegistry().getUnitTemplateByType(unitConfig.getType()).getServiceDescriptionList()) {
                        if (!serviceDescriptionsOnLocation.containsKey(serviceDescription.getType())) {
                            serviceDescriptionsOnLocation.put(serviceDescription.getType(), serviceDescription);
                        } else {
                            if (serviceDescriptionsOnLocation.get(serviceDescription.getType()).getPattern() == ServiceTemplate.ServicePattern.PROVIDER
                                    && serviceDescription.getPattern() == ServiceTemplate.ServicePattern.OPERATION) {
                                serviceDescriptionsOnLocation.put(serviceDescription.getType(), serviceDescription);
                            }
                        }
                    }
                }
                for (ServiceDescription serviceDescription : serviceDescriptionsOnLocation.values()) {
                    if (serviceDescription.getType() == ServiceType.COLOR_STATE_SERVICE
                            || serviceDescription.getType() == ServiceType.POWER_STATE_SERVICE
                            || serviceDescription.getType() == ServiceType.POWER_CONSUMPTION_STATE_SERVICE
                            || serviceDescription.getType() == ServiceType.TEMPERATURE_STATE_SERVICE
                            || serviceDescription.getType() == ServiceType.MOTION_STATE_SERVICE) {
                        LocationItemEntry entry = new LocationItemEntry(locationUnitConfig, serviceDescription);
                        itemEntryList.add(entry);
                        logger.debug("Added location entry [" + entry.buildStringRep() + "]");
                    }
                }
            }
            for (UnitConfig unitGroupUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.UNIT_GROUP)) {
                Set<ServiceType> serviceTypeSet = new HashSet<>();
                for (ServiceDescription serviceDescription : unitGroupUnitConfig.getUnitGroupConfig().getServiceDescriptionList()) {
                    logger.info("Generate ItemEntry for group[" + unitGroupUnitConfig.getLabel() + "] for service [" + serviceDescription.getType() + "]");
                    if (serviceDescription.getType() == ServiceType.COLOR_STATE_SERVICE || serviceDescription.getType() == ServiceType.POWER_STATE_SERVICE) {
                        if (!serviceTypeSet.contains(serviceDescription.getType())) {
                            UnitGroupItemEntry entry = new UnitGroupItemEntry(unitGroupUnitConfig, serviceDescription);
                            itemEntryList.add(entry);
                            logger.debug("Added unit group entry [" + entry.buildStringRep() + "]");
                            serviceTypeSet.add(serviceDescription.getType());
                        }
                    }
                }
            }

            // Scenes
            for (final UnitConfig sceneUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.SCENE)) {
                // Skip disabled scenes
                if (sceneUnitConfig.getEnablingState().getValue() == EnablingStateType.EnablingState.State.ENABLED) {
                    itemEntryList.add(new SceneItemEntry(sceneUnitConfig));
                }
            }

            final List<UnitConfig> deviceUnitConfigList = Registries.getUnitRegistry().getUnitConfigs(UnitType.DEVICE);

            for (UnitConfig deviceUnitConfig : deviceUnitConfigList) {

                // load device class
                DeviceClass deviceClass = Registries.getDeviceRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId());

                // ignore non openhab items
                if (!deviceClass.getBindingConfig().getBindingId().equalsIgnoreCase("OPENHAB")) {
                    continue;
                }

                // ignore non installed items
                if (deviceUnitConfig.getDeviceConfig().getInventoryState().getValue() != InventoryState.State.INSTALLED) {
                    continue;
                }

                final List<UnitConfig> dalUnitConfigList = new ArrayList<>();

                for (String unitId : deviceUnitConfig.getDeviceConfig().getUnitIdList()) {
                    dalUnitConfigList.add(Registries.getUnitRegistry().getUnitConfigById(unitId));
                }

                for (final UnitConfig unitConfig : dalUnitConfigList) {

                    // ignore disabled units
                    if (!unitConfig.getEnablingState().getValue().equals(EnablingStateType.EnablingState.State.ENABLED)) {
                        continue;
                    }

                    Set<ServiceType> serviceTypeSet = new HashSet<>();
                    for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.CONSUMER) {
                            continue;
                        }

                        if (serviceTypeSet.contains(serviceConfig.getServiceDescription().getType())) {
                            continue;
                        }

                        if (serviceConfig.getServiceDescription().getPattern() == ServicePattern.PROVIDER) {
                            if (unitHasServiceAsOperationService(unitConfig, serviceConfig.getServiceDescription().getType())) {
                                continue;
                            }
                        }

                        // TODO: fix that this has to be skipped, issue: https://github.com/openbase/bco.manager/issues/43
                        if (serviceConfig.getServiceDescription().getType() == ServiceType.TEMPERATURE_ALARM_STATE_SERVICE) {
                            continue;
                        }

                        serviceTypeSet.add(serviceConfig.getServiceDescription().getType());
                        try {
                            itemEntryList.add(new ServiceItemEntry(deviceClass, deviceUnitConfig.getMetaConfig(), unitConfig, serviceConfig));
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not generate item for Service[" + serviceConfig.getServiceDescription().getType().name() + "] of Unit[" + unitConfig.getLabel() + "]", ex), logger, LogLevel.WARN);
                        }
                    }
                }
            }

            for (final UnitConfig agentUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.AGENT)) {
                // Skip disabled agents
                if (agentUnitConfig.getEnablingState().getValue() == EnablingState.State.ENABLED) {
                    itemEntryList.add(new AgentItemEntry(agentUnitConfig, null));
                }
            }

            for (UnitConfig appUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.APP)) {
                // Skip disabled apps
                if (appUnitConfig.getEnablingState().getValue() == EnablingState.State.ENABLED) {
                    itemEntryList.add(new AppItemEntry(appUnitConfig));
                }
            }

            // sort items by command type and label
            Collections.sort(itemEntryList);

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
        return unitConfig.getServiceConfigList().stream().anyMatch((tmpServiceConfig) -> (tmpServiceConfig.getServiceDescription().getType() == serviceType
                && tmpServiceConfig.getServiceDescription().getPattern() == ServiceTemplate.ServicePattern.OPERATION));
    }
}
