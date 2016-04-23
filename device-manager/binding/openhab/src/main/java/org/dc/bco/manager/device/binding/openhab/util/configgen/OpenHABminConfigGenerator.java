/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.util.configgen;

/*
 * #%L
 * COMA DeviceManager Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2016 DivineCooperation
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

import static org.dc.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry.OPENHAB_BINDING_DEVICE_ID;
import static org.dc.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry.SERVICE_TEMPLATE_BINDING_TYPE;
import org.dc.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABDistribution;
import org.dc.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.dc.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABminZwaveConfig;
import org.dc.bco.manager.device.binding.openhab.util.configgen.xmlpaser.XMLParser;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPPrefix;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.printer.LogLevel;
import org.dc.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.dc.jul.processing.VariableProvider;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.io.File;
import java.io.IOException;
import java.util.List;
import nu.xom.Document;
import nu.xom.Element;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import rst.homeautomation.binding.BindingTypeHolderType;
import rst.homeautomation.device.DeviceConfigType;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;
import rst.homeautomation.state.InventoryStateType;

/**
 *
 * @author mpohling
 */
public class OpenHABminConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABminConfigGenerator.class);

    public static final long TIMEOUT = 15000;

    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationRegistryRemote locationRegistryRemote;

    public OpenHABminConfigGenerator() throws InstantiationException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.locationRegistryRemote = new LocationRegistryRemote();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        logger.info("init");
        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();
    }

    private void generate() throws CouldNotPerformException {
        try {
            File zwaveDb = JPService.getProperty(JPOpenHABminZwaveConfig.class).getValue();
            try {

                VariableProvider variableProvider;

                String zwaveNodeID;
                String openhabBindingType;
                File zwaveNodeConfigFile;

                logger.info("update zwave entries of HABmin zwave DB[" + zwaveDb + "] ...");

                List<DeviceConfigType.DeviceConfig> deviceConfigs = deviceRegistryRemote.getDeviceConfigs();

                for (DeviceConfigType.DeviceConfig deviceConfig : deviceConfigs) {
                    try {

                        // check openhab binding type
                        if (deviceRegistryRemote.getDeviceClassById(deviceConfig.getDeviceClassId()).getBindingConfig().getType() != BindingTypeHolderType.BindingTypeHolder.BindingType.OPENHAB) {
                            continue;
                        }

                        // check if zwave
                        variableProvider = new MetaConfigVariableProvider("BindingConfigVariableProvider", deviceRegistryRemote.getDeviceClassById(deviceConfig.getDeviceClassId()).getBindingConfig().getMetaConfig());
                        openhabBindingType = variableProvider.getValue(SERVICE_TEMPLATE_BINDING_TYPE);
                        if (!"zwave".equals(openhabBindingType)) {
                            continue;
                        }

                        // check if installed
                        if (deviceConfig.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                            continue;
                        }

                        variableProvider = new MetaConfigVariableProvider("DeviceConfigVariableProvider", deviceConfig.getMetaConfig());
                        zwaveNodeID = variableProvider.getValue(OPENHAB_BINDING_DEVICE_ID);
                        zwaveNodeConfigFile = new File(zwaveDb, "node" + zwaveNodeID + ".xml");

                        if (!zwaveNodeConfigFile.exists()) {
                            logger.warn("Could not detect zwave node config File[" + zwaveNodeConfigFile + "]! Skip device...");
                            continue;
                        }

                        updateZwaveNodeConfig(zwaveNodeConfigFile, deviceConfig);
                        logger.info("Successful updated zwave Node[" + zwaveNodeID + "] of Device[" + deviceConfig.getLabel() + "].");
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update node entry for Device[" + deviceConfig.getLabel() + "]!", ex), logger, LogLevel.ERROR);
                    }
                }

            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not update zwave Entries[" + JPService.getProperty(JPOpenHABItemConfig.class).getValue() + "].", ex);
            }
        } catch (JPServiceException ex) {
            throw new CouldNotPerformException("Could not update zwave entries!", ex);
        }
    }

    public void updateZwaveNodeConfig(final File zwaveNodeConfigFile, final DeviceConfig deviceConfig) throws CouldNotPerformException {
        try {

            // load
            Document doc = XMLParser.createDocumentFromFile(zwaveNodeConfigFile);
            Element nodeElement = doc.getRootElement();

            // remove old values
            try {
                nodeElement.removeChild(XMLParser.parseOneChildElement("name", nodeElement));
            } catch (Exception ex) {

            }
            try {
                nodeElement.removeChild(XMLParser.parseOneChildElement("location", nodeElement));
            } catch (Exception ex) {

            }

            // create new
            Element nameElement = new Element("name");
            Element locationElement = new Element("location");

            // add values
            nameElement.appendChild(deviceConfig.getLabel() + " " + locationRegistryRemote.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId()).getLabel());
            locationElement.appendChild(locationRegistryRemote.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId()).getLabel());

            // store back
            nodeElement.appendChild(nameElement);
            nodeElement.appendChild(locationElement);

            // save
            try {
                FileUtils.writeStringToFile(zwaveNodeConfigFile, XMLParser.normalizeFormattingAsString(doc));
            } catch (IOException ex) {
                throw new CouldNotPerformException("Could not save zwave node config!", ex);
            }

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not update zwave node config of Device[" + deviceConfig.getLabel() + "]!", ex);
        }
    }

    private void shutdown() {
        logger.info("shutdown");
        deviceRegistryRemote.shutdown();
        locationRegistryRemote.shutdown();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        JPService.setApplicationName("dal-openhabmin-zwave-config-updater");
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPOpenHABminZwaveConfig.class);
        JPService.registerProperty(JPOpenHABDistribution.class);
        JPService.parseAndExitOnError(args);

        try {
            final OpenHABminConfigGenerator openHABConfigGenerator = new OpenHABminConfigGenerator();
            openHABConfigGenerator.init();
            openHABConfigGenerator.generate();
            openHABConfigGenerator.shutdown();
            logger.info(JPService.getApplicationName() + " successfully started.");
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " finished.");
        System.exit(0);
    }

    private void createDocumentFromFile(File zwaveNodeConfig) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
