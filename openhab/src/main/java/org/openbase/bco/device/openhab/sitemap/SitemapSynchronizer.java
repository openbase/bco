package org.openbase.bco.device.openhab.sitemap;

import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.openbase.bco.device.openhab.jp.JPOpenHABSitemap;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.iface.Launchable;
import org.openbase.jul.iface.VoidInitializable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.RecurrenceEventFilter;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;

import java.io.File;
/*
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2019 openbase.org
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

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class SitemapSynchronizer implements Launchable<Void>, VoidInitializable {

    public static final long TIMEOUT = 15000;
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SitemapSynchronizer.class);
    private final SitemapGenerator sidemapGenerator;
    private final RecurrenceEventFilter<Void> recurrenceGenerationFilter;
    private boolean active;

    public SitemapSynchronizer() throws InstantiationException, InterruptedException {
        try {
            Registries.waitForData();
            this.sidemapGenerator = new SitemapGenerator();
            this.recurrenceGenerationFilter = new RecurrenceEventFilter<Void>(TIMEOUT) {

                @Override
                public void relay() throws Exception {
                    internalGenerate();
                }

            };

            FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(JPService.getProperty(JPOpenHABSitemap.class).getValue().getParent());
            fileAlterationObserver.initialize();
            fileAlterationObserver.addListener(new FileAlterationListenerAdaptor() {

                @Override
                public void onFileDelete(File file) {
                    logger.warn("Detect sitemap file deletion!");

                    try {
                        generate();
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not regenerate sitemap file after deletion!", ex, logger);
                    }
                }
            });

            final FileAlterationMonitor monitor = new FileAlterationMonitor(10000);
            monitor.addObserver(fileAlterationObserver);
            monitor.start();

            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

                @Override
                public void run() {
                    shutdown();
                    try {
                        monitor.stop();
                    } catch (Exception ex) {
                        ExceptionPrinter.printHistory(ex, logger);
                    }
                }
            }));

        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void init() throws InitializationException, InterruptedException {
        try {
            Registries.waitForData();

            Registries.getUnitRegistry().addDataObserver((final DataProvider<UnitRegistryData> source, UnitRegistryData data) -> {
                generate();
            });
            generate();
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void generate() throws CouldNotPerformException {
        recurrenceGenerationFilter.trigger();
    }

    private synchronized void internalGenerate() throws CouldNotPerformException, InterruptedException {
        try {
            try {
                logger.info("generate Sitemap[" + JPService.getProperty(JPOpenHABSitemap.class).getValue() + "] ...");
                sidemapGenerator.generate();
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not generate ItemConfig[" + JPService.getProperty(JPOpenHABSitemap.class).getValue() + "].", ex);
            }
        } catch (JPServiceException ex) {
            throw new CouldNotPerformException("Could not generate ItemConfig!", ex);
        }
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        active = true;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void shutdown() {
    }
}
