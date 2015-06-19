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
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.lm.remote.LocationRegistryRemote;
import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.LoggerFactory;
import rst.homeautomation.device.DeviceRegistryType;
import rst.spatial.LocationRegistryType;

/**
 *
 * @author mpohling
 */
public class OpenHABConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABConfigGenerator.class);

    private final OpenHABItemConfigGenerator itemConfigGenerator;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationRegistryRemote locationRegistryRemote;

    public OpenHABConfigGenerator() throws InstantiationException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.itemConfigGenerator = new OpenHABItemConfigGenerator(deviceRegistryRemote, locationRegistryRemote);
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
        itemConfigGenerator.init();

        this.deviceRegistryRemote.addObserver(new Observer<DeviceRegistryType.DeviceRegistry>() {

            @Override
            public void update(Observable<DeviceRegistryType.DeviceRegistry> source, DeviceRegistryType.DeviceRegistry data) throws Exception {
                try {
                    generate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
                }
            }
        });
        this.locationRegistryRemote.addObserver(new Observer<LocationRegistryType.LocationRegistry>() {

            @Override
            public void update(Observable<LocationRegistryType.LocationRegistry> source, LocationRegistryType.LocationRegistry data) throws Exception {
                try {
                    generate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
                }
            }
        });
    }

    private synchronized void generate() throws CouldNotPerformException {
        try {
            logger.info("generate");
            itemConfigGenerator.generate();

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate ex.", ex);
        }
    }

    private void shutdown() {
        logger.info("shutdown");
        try {
            itemConfigGenerator.generate();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
        deviceRegistryRemote.shutdown();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        JPService.setApplicationName("dal-openhab-config-generator");
        JPService.registerProperty(JPOpenHABItemConfig.class, new File("/tmp/itemconfig.txt"));
        JPService.parseAndExitOnError(args);

        try {
            final OpenHABConfigGenerator openHABConfigGenerator = new OpenHABConfigGenerator();
            openHABConfigGenerator.init();
            openHABConfigGenerator.generate();
            
            FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(JPService.getProperty(JPOpenHABItemConfig.class).getValue().getParent());
            fileAlterationObserver.initialize();
            fileAlterationObserver.addListener(new FileAlterationListenerAdaptor() {

                @Override
                public void onFileDelete(File file) {
                    try {
                        logger.info("Detect config file deletion!");
                        openHABConfigGenerator.generate();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistoryAndReturnThrowable(logger, new CouldNotPerformException("Could not regenerate config!", ex));
                    }
                }
            });

            final FileAlterationMonitor monitor = new FileAlterationMonitor(10000);
            monitor.addObserver(fileAlterationObserver);
            monitor.start();
            
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                @Override
                public void run() {
                    openHABConfigGenerator.shutdown();
                    try {
                        monitor.stop();
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
                    }
                }
            }));


        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
    }
}
