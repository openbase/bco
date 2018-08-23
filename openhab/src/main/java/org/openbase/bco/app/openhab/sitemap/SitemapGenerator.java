package org.openbase.bco.app.openhab.sitemap;

import org.apache.commons.io.FileUtils;
import org.openbase.bco.app.openhab.jp.JPOpenHABSitemap;
import org.openbase.bco.app.openhab.sitemap.element.RootLocationElement;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.iface.DefaultInitializable;
import org.openbase.jul.iface.Shutdownable;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

public class SitemapGenerator implements DefaultInitializable, Shutdownable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SitemapGenerator.class);

    @Override
    public void init() throws InitializationException, InterruptedException {

    }

    @Override
    public void shutdown() {

    }

    public void generate() throws CouldNotPerformException {
        logger.info("generate sitemap...");
        try {

            serializeToFile();
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Could not generate sitemap!", ex);
        }
    }


    private void serializeToFile() throws CouldNotPerformException {
        try {
            final File configFile = JPService.getProperty(JPOpenHABSitemap.class).getValue();

            FileUtils.writeStringToFile(configFile, new RootLocationElement().getElement(), Charset.forName("UTF8"), false);

            logger.info("Sitemap[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize sitemap to file!", ex);
        }
    }
}
