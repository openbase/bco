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
import org.dc.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABConfiguration;
import org.dc.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABDistribution;
import org.dc.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.dc.bco.registry.device.remote.DeviceRegistryRemote;
import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPServiceException;
import org.dc.jps.preset.JPPrefix;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.InitializationException;
import org.dc.jul.exception.InstantiationException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.schedule.RecurrenceEventFilter;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.dc.bco.registry.agent.remote.AgentRegistryRemote;
import org.dc.bco.registry.app.remote.AppRegistryRemote;
import org.dc.bco.registry.scene.remote.SceneRegistryRemote;
import org.dc.jul.pattern.Observable;
import org.slf4j.LoggerFactory;
import rst.homeautomation.control.agent.AgentRegistryType.AgentRegistry;
import rst.homeautomation.control.app.AppRegistryType.AppRegistry;
import rst.homeautomation.control.scene.SceneRegistryType.SceneRegistry;
import rst.homeautomation.device.DeviceRegistryType.DeviceRegistry;
import rst.spatial.LocationRegistryType.LocationRegistry;

/**
 * //TODO: openHAB config generator should maybe become a project on its on. It
 * does not belong to the device manager since it also generates entries for
 * scenes, agents etc
 *
 * @author mpohling
 */
public class OpenHABConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABConfigGenerator.class);

    public static final long TIMEOUT = 15000;

    private final OpenHABItemConfigGenerator itemConfigGenerator;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final LocationRegistryRemote locationRegistryRemote;
    private final SceneRegistryRemote sceneRegistryRemote;
    private final AgentRegistryRemote agentRegistryRemote;
    private final AppRegistryRemote appRegistryRemote;
    private final RecurrenceEventFilter recurrenceGenerationFilter;

    public OpenHABConfigGenerator() throws InstantiationException {
        try {
            this.deviceRegistryRemote = new DeviceRegistryRemote();
            this.locationRegistryRemote = new LocationRegistryRemote();
            this.sceneRegistryRemote = new SceneRegistryRemote();
            this.agentRegistryRemote = new AgentRegistryRemote();
            this.appRegistryRemote = new AppRegistryRemote();
            this.itemConfigGenerator = new OpenHABItemConfigGenerator(deviceRegistryRemote, locationRegistryRemote, sceneRegistryRemote, agentRegistryRemote, appRegistryRemote);
            this.recurrenceGenerationFilter = new RecurrenceEventFilter(TIMEOUT) {

                @Override
                public void relay() throws Exception {
                    internalGenerate();
                }

            };

        } catch (CouldNotPerformException | InterruptedException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        logger.info("init");
        deviceRegistryRemote.init();
        deviceRegistryRemote.activate();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();
        sceneRegistryRemote.init();
        sceneRegistryRemote.activate();
        agentRegistryRemote.init();
        agentRegistryRemote.activate();
        appRegistryRemote.init();
        appRegistryRemote.activate();
        itemConfigGenerator.init();

        this.deviceRegistryRemote.addObserver((Observable<DeviceRegistry> source, DeviceRegistry data) -> {
            generate();
        });
        this.locationRegistryRemote.addObserver((Observable<LocationRegistry> source, LocationRegistry data) -> {
            generate();
        });
        this.sceneRegistryRemote.addObserver((Observable<SceneRegistry> source, SceneRegistry data) -> {
            generate();
        });
        this.agentRegistryRemote.addObserver((Observable<AgentRegistry> source, AgentRegistry data) -> {
            generate();
        });
        this.appRegistryRemote.addObserver((Observable<AppRegistry> source, AppRegistry data) -> {
            generate();
        });
    }

    public void generate() {
        recurrenceGenerationFilter.trigger();
    }

    private synchronized void internalGenerate() throws CouldNotPerformException {
        try {
            try {
                logger.info("generate ItemConfig[" + JPService.getProperty(JPOpenHABItemConfig.class).getValue() + "] ...");
                itemConfigGenerator.generate();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not generate ItemConfig[" + JPService.getProperty(JPOpenHABItemConfig.class).getValue() + "].", ex);
            }
        } catch (JPServiceException ex) {
            throw new CouldNotPerformException("Could not generate ItemConfig!", ex);
        }
    }

    private void shutdown() {
        logger.info("shutdown");
        itemConfigGenerator.shutdown();
        deviceRegistryRemote.shutdown();
        locationRegistryRemote.shutdown();
        sceneRegistryRemote.shutdown();
        agentRegistryRemote.shutdown();
        appRegistryRemote.shutdown();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        JPService.setApplicationName("dal-openhab-config-generator");
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPOpenHABItemConfig.class);
        JPService.registerProperty(JPOpenHABDistribution.class);
        JPService.registerProperty(JPOpenHABConfiguration.class);
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
                    logger.warn("Detect config file deletion!");
                    openHABConfigGenerator.generate();
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
                        ExceptionPrinter.printHistory(ex, logger);
                    }
                }
            }));

        } catch (Exception ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(ex, logger);
        }
        logger.info(JPService.getApplicationName() + " successfully started.");
    }
}
