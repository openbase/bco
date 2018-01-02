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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import nu.xom.Document;
import nu.xom.Element;
import org.apache.commons.io.FileUtils;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry.OPENHAB_BINDING_DEVICE_ID;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry.SERVICE_TEMPLATE_BINDING_TYPE;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABDistribution;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABminZwaveConfig;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPPrefix;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.extension.xml.exception.XMLParsingException;
import org.openbase.jul.extension.xml.processing.XMLProcessor;
import org.openbase.jul.processing.VariableProvider;
import org.slf4j.LoggerFactory;
import rst.domotic.state.InventoryStateType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABminConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABminConfigGenerator.class);

    public static final long TIMEOUT = 15000;

    public OpenHABminConfigGenerator() throws InstantiationException {
    }

    private void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        logger.info("init");
        Registries.getUnitRegistry().waitForData();
        Registries.getDeviceRegistry().waitForData();
        Registries.getLocationRegistry().waitForData();
    }

    private void generate() throws CouldNotPerformException, InterruptedException {
        try {
            File zwaveDb = JPService.getProperty(JPOpenHABminZwaveConfig.class).getValue();
            try {

                VariableProvider variableProvider;

                String zwaveNodeID;
                String openhabBindingType;
                File zwaveNodeConfigFile;

                logger.info("update zwave entries of HABmin zwave DB[" + zwaveDb + "] ...");

                List<UnitConfig> deviceUnitConfigs = Registries.getUnitRegistry().getData().getDeviceUnitConfigList();

                for (UnitConfig deviceUnitConfig : deviceUnitConfigs) {
                    try {

                        // check openhab binding type
                        if (!Registries.getDeviceRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId()).getBindingConfig().getBindingId().equals("OPENHAB")) {
                            continue;
                        }

                        // check if zwave
                        variableProvider = new MetaConfigVariableProvider("BindingConfigVariableProvider", Registries.getDeviceRegistry().getDeviceClassById(deviceUnitConfig.getDeviceConfig().getDeviceClassId()).getBindingConfig().getMetaConfig());
                        openhabBindingType = variableProvider.getValue(SERVICE_TEMPLATE_BINDING_TYPE);
                        if (!"zwave".equals(openhabBindingType)) {
                            continue;
                        }

                        // check if installed
                        if (deviceUnitConfig.getDeviceConfig().getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
                            continue;
                        }

                        variableProvider = new MetaConfigVariableProvider("DeviceConfigVariableProvider", deviceUnitConfig.getMetaConfig());
                        zwaveNodeID = variableProvider.getValue(OPENHAB_BINDING_DEVICE_ID);
                        zwaveNodeConfigFile = new File(zwaveDb, "node" + zwaveNodeID + ".xml");

                        if (!zwaveNodeConfigFile.exists()) {
                            logger.warn("Could not detect zwave node config File[" + zwaveNodeConfigFile + "]! Skip device...");
                            continue;
                        }

                        updateZwaveNodeConfig(zwaveNodeConfigFile, deviceUnitConfig);
                        logger.info("Successful updated zwave Node[" + zwaveNodeID + "] of Device[" + deviceUnitConfig.getLabel() + "].");
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not update node entry for Device[" + deviceUnitConfig.getLabel() + "]!", ex), logger, LogLevel.ERROR);
                    }
                }

            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not update zwave Entries[" + JPService.getProperty(JPOpenHABItemConfig.class).getValue() + "].", ex);
            }
        } catch (JPServiceException ex) {
            throw new CouldNotPerformException("Could not update zwave entries!", ex);
        }
    }

    public void updateZwaveNodeConfig(final File zwaveNodeConfigFile, final UnitConfig deviceDeviceConfig) throws CouldNotPerformException, InterruptedException {
        try {

            // load
            Document doc = XMLProcessor.createDocumentFromFile(zwaveNodeConfigFile);
            Element nodeElement = doc.getRootElement();

            // remove old values
            try {
                nodeElement.removeChild(XMLProcessor.parseOneChildElement("name", nodeElement));
            } catch (Exception ex) {
                // ignore if not exists
            }
            try {
                nodeElement.removeChild(XMLProcessor.parseOneChildElement("location", nodeElement));
            } catch (Exception ex) {
                // ignore if not exists
            }

            // create new
            Element nameElement = new Element("name");
            Element locationElement = new Element("location");

            // add values
            nameElement.appendChild(deviceDeviceConfig.getLabel() + " " + Registries.getLocationRegistry().getLocationConfigById(deviceDeviceConfig.getPlacementConfig().getLocationId()).getLabel());
            locationElement.appendChild(Registries.getLocationRegistry().getLocationConfigById(deviceDeviceConfig.getPlacementConfig().getLocationId()).getLabel());

            // store back
            nodeElement.appendChild(nameElement);
            nodeElement.appendChild(locationElement);

            // save
            try {
                FileUtils.writeStringToFile(zwaveNodeConfigFile, XMLProcessor.normalizeFormattingAsString(doc), StandardCharsets.UTF_8);
            } catch (final IOException ex) {
                throw new CouldNotPerformException("Could not save zwave node config!", ex);
            }

        } catch (final XMLParsingException | IOException | CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not update zwave node config of Device[" + deviceDeviceConfig.getLabel() + "]!", ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        JPService.setApplicationName("bco-openhabmin-zwave-config-updater");
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPOpenHABminZwaveConfig.class);
        JPService.registerProperty(JPOpenHABDistribution.class);
        JPService.parseAndExitOnError(args);

        try {
            final OpenHABminConfigGenerator openHABConfigGenerator = new OpenHABminConfigGenerator();
            openHABConfigGenerator.init();
            openHABConfigGenerator.generate();
            logger.info(JPService.getApplicationName() + " successfully started.");
        } catch (Exception ex) {
            ExceptionPrinter.printHistory(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " finished.");
        System.exit(0);
    }
}
