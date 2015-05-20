/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util;

import de.citec.dal.bindings.openhab.util.jp.JPOpenHABItemConfig;
import de.citec.jps.core.JPService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import java.io.File;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 */
public class OpenHABConfigGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OpenHABConfigGenerator.class);

    private final OpenHABItemConfigGenerator itemConfigGenerator;

    public OpenHABConfigGenerator() throws InstantiationException {
        try {
            this.itemConfigGenerator = new OpenHABItemConfigGenerator();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void init() throws InitializationException, InterruptedException, CouldNotPerformException {
        logger.info("init");
        itemConfigGenerator.init();
    }

    private void generate() throws CouldNotPerformException {
        logger.info("generate");
        itemConfigGenerator.generate();
    }

    private void shutdown() throws CouldNotPerformException {
        logger.info("shutdown");
        itemConfigGenerator.generate();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        JPService.setApplicationName("dal-openhab-config-generator");
        JPService.registerProperty(JPOpenHABItemConfig.class, new File("/tmp/itemconfig.txt"));
        JPService.parseAndExitOnError(args);

        OpenHABConfigGenerator openHABConfigGenerator = null;
        try {
            openHABConfigGenerator = new OpenHABConfigGenerator();
            openHABConfigGenerator.init();
            openHABConfigGenerator.generate();
        } catch (Exception ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        } finally {
            if (openHABConfigGenerator != null) {
                openHABConfigGenerator.shutdown();
            }
        }
    }
}
