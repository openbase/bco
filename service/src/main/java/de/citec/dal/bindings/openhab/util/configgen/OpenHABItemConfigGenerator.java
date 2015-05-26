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
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InitializationException;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.OpenHABBindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
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
            List<LocationConfigType.LocationConfig> locationConfigList = locationRegistryRemote.getData().getLocationConfigList();
            for (LocationConfig locationConfig : locationConfigList) {
                groupEntryList.add(new GroupEntry(locationConfig));
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate group entries.", ex);
        }
    }

    private void generateItemEntries() throws CouldNotPerformException {
        try {
            List<DeviceConfigType.DeviceConfig> deviceConfigList = deviceRegistryRemote.getData().getDeviceConfigList();
            for (DeviceConfig deviceConfig : deviceConfigList) {
                if (deviceConfig.getDeviceClass().getBindingConfig().getType() != BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB) {
                    continue;
                }
                for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
                    for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                        BindingServiceConfigType.BindingServiceConfig bindingServiceConfig = serviceConfig.getBindingServiceConfig();

                        OpenHABBindingServiceConfigType.OpenHABBindingServiceConfig openhabBindingServiceConfig = bindingServiceConfig.getOpenhabBindingServiceConfig();
                        itemEntryList.add(new ItemEntry(unitConfig, serviceConfig, openhabBindingServiceConfig));
                    }
                }
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate group entries.", ex);
        }
    }

    private void serializeToFile() throws CouldNotPerformException {
        try {
            String configAsString = "";

            File configFile = JPService.getProperty(JPOpenHABItemConfig.class).getValue();

            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === DAL AUTO GENERATED GROUP ENTRIES ============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            for (GroupEntry entry : groupEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();

            configAsString += "/* =================================================== */" + System.lineSeparator();
            configAsString += "/* === DAL AUTO GENERATED ITEM ENTRIES =============== */" + System.lineSeparator();
            configAsString += "/* =================================================== */" + System.lineSeparator();
            for (ItemEntry entry : itemEntryList) {
                configAsString += entry.buildStringRep() + System.lineSeparator();
            }
            configAsString += System.lineSeparator();

            FileUtils.writeStringToFile(configFile, configAsString, false);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize itemconfig to file!", ex);
        }
    }

    public void shutdown() {

    }
}
