/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util;

import de.citec.dal.bindings.openhab.util.jp.JPOpenHABItemConfig;
import de.citec.dal.hal.service.ServiceType;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.InitializationException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceClassType.DeviceClass;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.service.BindingServiceConfigType;
import rst.homeautomation.service.OpenHABBindingServiceConfigType;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTypeHolderType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author mpohling
 */
public class OpenHABItemConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABItemConfigGenerator.class);

    private final List<String> configList;
    private final DeviceRegistryRemote deviceRegistryRemote;

    public OpenHABItemConfigGenerator() throws InstantiationException {
        try {
            this.configList = new ArrayList<>();
            this.deviceRegistryRemote = new DeviceRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public void init() throws InitializationException, InterruptedException, CouldNotPerformException {

        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();
    }

    public void generate() throws CouldNotPerformException {
        logger.info("generate item config");
        try {
            generateItemGroups();
            generateItemEntries();
            print();
            serializeToFile();
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not generate item config", ex);
        }
    }

    private void generateItemGroups() {

    }

    private void generateItemEntries() throws CouldNotPerformException {
        List<DeviceConfigType.DeviceConfig> deviceConfigList = deviceRegistryRemote.getData().getDeviceConfigList();

        DeviceClass deviceClass;
        for (DeviceConfig deviceConfig : deviceConfigList) {
            deviceClass = deviceConfig.getDeviceClass();
            for (UnitConfig unitConfig : deviceConfig.getUnitConfigList()) {
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                    BindingServiceConfigType.BindingServiceConfig bindingServiceConfig = serviceConfig.getBindingServiceConfig();
                    if (bindingServiceConfig.getType() != BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB) {
                        continue;
                    }
                    OpenHABBindingServiceConfigType.OpenHABBindingServiceConfig openhabBindingServiceConfig = bindingServiceConfig.getOpenhabBindingServiceConfig();

                    String serviceEntry = "";
                    serviceEntry += getCommand(serviceConfig.getType());
                    serviceEntry += "   ";

                    // id !?
                    serviceEntry += openhabBindingServiceConfig.getItemId();
                    serviceEntry += "   ";

                    // label
                    serviceEntry += "\"" + unitConfig.getLabel() + "\"";
                    serviceEntry += "   ";

                    // icon !?
//                    serviceEntry += "<" + unitConfig. + ">";
                    serviceEntry += "   ";
                    // groups
                    serviceEntry += "(" + unitConfig.getTemplate().getType().name().toLowerCase() + "," + unitConfig.getPlacementConfig().getLocationId().toLowerCase() + ")";
                    serviceEntry += "   ";

                    // hardware
                    serviceEntry += "{ " + openhabBindingServiceConfig.getItemHardwareConfig() + " }";

                    configList.add(serviceEntry);

                }

            }
        }
    }

    private String getCommand(ServiceTypeHolderType.ServiceTypeHolder.ServiceType type) {
        switch (type) {
            case COLOR_SERVICE:
                return "Color";
            case OPENING_RATIO_PROVIDER:
                return "Number";
            case BATTERY_PROVIDER:
            case SHUTTER_SERVICE:
                return "Percent";
            case POWER_SERVICE:
                return "Switch";
            case TEMPERATURE_PROVIDER:
            case MOTION_PROVIDER:
            case TAMPER_PROVIDER:
                return "Number";
            case BRIGHTNESS_PROVIDER:
            case BRIGHTNESS_SERVICE:
            case DIM_PROVIDER:
            case DIM_SERVICE:
                return "Dimmer";
            default:
//                throw new AssertionError("Unkown Service Type: " + type);
                logger.warn("Unkown Service Type: " + type);
                return "";

        }
    }

    private void serializeToFile() throws CouldNotPerformException {
        try {
            String configAsString = "";

            File configFile = JPService.getProperty(JPOpenHABItemConfig.class).getValue();

            for (String line : configList) {
                configAsString += line + System.lineSeparator();
            }
            FileUtils.writeStringToFile(configFile, configAsString, false);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize itemconfig to file!", ex);
        }
    }

    private void print() {
        logger.info("##### ITEM CONFIG #####");
        for (String configLine : configList) {
            logger.info(configLine);
        }
        logger.info("#######################");
    }

    public void shutdown() {
        deviceRegistryRemote.shutdown();
    }
}
