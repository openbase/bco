package org.openbase.bco.manager.device.binding.openhab.util.configgen;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
 * %%
 * Copyright (C) 2015 - 2017 openbase.org
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
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABConfiguration;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABDistribution;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.jp.JPOpenHABItemConfig;
import org.openbase.bco.registry.device.remote.DeviceRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jps.preset.JPPrefix;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.openbase.bco.registry.location.remote.LocationRegistryRemote;
import java.io.File;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.openbase.bco.registry.agent.remote.AgentRegistryRemote;
import org.openbase.bco.registry.app.remote.AppRegistryRemote;
import org.openbase.bco.registry.scene.remote.SceneRegistryRemote;
import org.openbase.bco.registry.unit.remote.UnitRegistryRemote;
import org.openbase.jul.pattern.Observable;
import org.slf4j.LoggerFactory;
import rst.domotic.registry.AgentRegistryDataType.AgentRegistryData;
import rst.domotic.registry.AppRegistryDataType.AppRegistryData;
import rst.domotic.registry.SceneRegistryDataType.SceneRegistryData;
import rst.domotic.registry.DeviceRegistryDataType.DeviceRegistryData;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.registry.LocationRegistryDataType.LocationRegistryData;

/**
 * //TODO: openHAB config generator should maybe become a project on its on. It
 * does not belong to the device manager since it also generates entries for
 * scenes, agents etc
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABConfigGenerator.class);

    public static final long TIMEOUT = 15000;

    private final OpenHABItemConfigGenerator itemConfigGenerator;
    private final DeviceRegistryRemote deviceRegistryRemote;
    private final UnitRegistryRemote unitRegistryRemote;
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
            this.unitRegistryRemote = new UnitRegistryRemote();
            this.itemConfigGenerator = new OpenHABItemConfigGenerator(deviceRegistryRemote, unitRegistryRemote, locationRegistryRemote, sceneRegistryRemote, agentRegistryRemote, appRegistryRemote);
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
        unitRegistryRemote.init();
        unitRegistryRemote.activate();

        deviceRegistryRemote.waitForData();
        locationRegistryRemote.waitForData();
        sceneRegistryRemote.waitForData();
        agentRegistryRemote.waitForData();
        appRegistryRemote.waitForData();
        unitRegistryRemote.waitForData();

        this.deviceRegistryRemote.addDataObserver((Observable<DeviceRegistryData> source, DeviceRegistryData data) -> {
            generate();
        });
        this.locationRegistryRemote.addDataObserver((Observable<LocationRegistryData> source, LocationRegistryData data) -> {
            generate();
        });
        this.sceneRegistryRemote.addDataObserver((Observable<SceneRegistryData> source, SceneRegistryData data) -> {
            generate();
        });
        this.agentRegistryRemote.addDataObserver((Observable<AgentRegistryData> source, AgentRegistryData data) -> {
            generate();
        });
        this.appRegistryRemote.addDataObserver((Observable<AppRegistryData> source, AppRegistryData data) -> {
            generate();
        });
        this.unitRegistryRemote.addDataObserver((Observable<UnitRegistryData> source, UnitRegistryData data) -> {
            generate();
        });
    }

    public void generate() {
        recurrenceGenerationFilter.trigger();
    }

    private synchronized void internalGenerate() throws CouldNotPerformException, InterruptedException {
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
        unitRegistryRemote.shutdown();
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
