/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen;

import static de.citec.dal.bindings.openhab.util.configgen.ItemEntry.OPENHAB_BINDING_DEVICE_ID;
import de.citec.dal.bindings.openhab.util.configgen.jp.JPOpenHABDistribution;
import de.citec.dal.bindings.openhab.util.configgen.jp.JPOpenHABItemConfig;
import de.citec.dal.bindings.openhab.util.configgen.jp.JPOpenHABminZwaveConfig;
import de.citec.dal.bindings.openhab.util.configgen.xmlpaser.XMLParser;
import de.citec.dm.remote.DeviceRegistryRemote;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPPrefix;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.rst.processing.MetaConfigVariableProvider;
import de.citec.jul.processing.VariableProvider;
import de.citec.lm.remote.LocationRegistryRemote;
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

            VariableProvider variableProvider;
            File zwaveDb = JPService.getProperty(JPOpenHABminZwaveConfig.class).getValue();
            String zwaveNodeID;
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
//                    if (deviceConfig.getInventoryState().getValue() != InventoryStateType.InventoryState.State.INSTALLED) {
//                        continue;
//                    }

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

        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not update zwave Entries[" + JPService.getProperty(JPOpenHABItemConfig.class).getValue() + "].", ex);
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
            nameElement.appendChild(deviceConfig.getLabel());
            locationElement.appendChild(deviceConfig.getLabel() + " " + locationRegistryRemote.getLocationConfigById(deviceConfig.getPlacementConfig().getLocationId()).getLabel());

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
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger, LogLevel.ERROR);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }

    private void createDocumentFromFile(File zwaveNodeConfig) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
