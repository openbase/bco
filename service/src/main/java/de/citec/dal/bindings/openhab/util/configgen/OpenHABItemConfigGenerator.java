/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen;

import de.citec.dal.bindings.openhab.util.configgen.jp.JPOpenHABItemConfig;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.processing.StringProcessor;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.InventoryStateType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.spatial.LocationConfigType;
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author mpohling
 */
public class OpenHABItemConfigGenerator {

    public static final int TAB_SIZE = 4;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABItemConfigGenerator.class);

    private final List<ItemEntry> itemEntryList;
    private final List<GroupEntry> groupEntryList;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationRegistryRemote locationRegistryRemote;

    public OpenHABItemConfigGenerator(final DeviceRegistryRemote deviceRegistryRemote, final LocationRegistryRemote locationRegistryRemote) throws InstantiationException {
        try {
            this.itemEntryList = new ArrayList<>();
            this.groupEntryList = new ArrayList<>();
            this.deviceRegistryRemote = deviceRegistryRemote;
            this.locationRegistryRemote = locationRegistryRemote;
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
            ItemEntry.reset();
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
            List<LocationConfigType.LocationConfig> locationConfigList = locationRegistryRemote.getData().getLocationConfigList();
            for (LocationConfig locationConfig : locationConfigList) {
                groupEntry = new GroupEntry(locationConfig);
                groupEntryList.add(new GroupEntry(locationConfig));

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
        GroupEntry overviewGroupEntry = new GroupEntry("overview", "Übersicht", "settings", rootGroupEntry);
        groupEntryList.add(overviewGroupEntry);

        for (UnitType unitType : UnitType.values()) {
            if(unitType.equals(UnitType.UNKNOWN)) {
                continue;
            }
            String unitLabel = StringProcessor.transformUpperCaseToCamelCase(unitType.name());
            groupEntryList.add(new GroupEntry(unitLabel, unitLabel, unitLabel, overviewGroupEntry));
        }

//        for (ServiceType serviceType : ServiceType.values()) {
//            groupEntryList.add(new GroupEntry(serviceType.name(), serviceType.name(), "", rootGroupEntry));
//        }
    }

    private void generateItemEntries() throws CouldNotPerformException {
        try {
            List<DeviceConfigType.DeviceConfig> deviceConfigList = deviceRegistryRemote.getData().getDeviceConfigList();

            for (DeviceConfig deviceConfig : deviceConfigList) {

                // load device class
                DeviceClass deviceClass = deviceRegistryRemote.getDeviceClassById(deviceConfig.getDeviceClassId());

                // ignore non openhab items
                if (deviceClass.getBindingConfig().getType() != BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB) {
                    continue;
                }

                // ignore non installed items
                if (deviceConfig.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                    continue;
                }

                for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
                    for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        try {
                            itemEntryList.add(new ItemEntry(deviceClass, deviceConfig, unitConfig, serviceConfig));
                        } catch (Exception ex) {
                            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not generate item for Service[" + serviceConfig.getType().name() + "] of Unit[" + unitConfig.getId() + "]", ex), logger, LogLevel.ERROR);
                        }
                    }
                }
            }

            // sort items by command type and label
            Collections.sort(itemEntryList, new Comparator<ItemEntry>() {

                @Override
                public int compare(ItemEntry o1, ItemEntry o2) {
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
            configAsString += "/* === DAL AUTO GENERATED GROUP ENTRIES ============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += System.lineSeparator();
            for (GroupEntry entry : groupEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();
            configAsString += System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === DAL AUTO GENERATED ITEM ENTRIES =============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += System.lineSeparator();
            for (ItemEntry entry : itemEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();

            FileUtils.writeStringToFile(configFile, configAsString, false);

            logger.info("ItemConfig[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize itemconfig to file!", ex);
        }
    }

    public void shutdown() {

    }
}
