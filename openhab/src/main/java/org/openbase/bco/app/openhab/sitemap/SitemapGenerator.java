package org.openbase.bco.app.openhab.sitemap;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.apache.commons.io.FileUtils;
import org.openbase.bco.app.openhab.jp.JPOpenHABSitemap;
import org.openbase.bco.app.openhab.sitemap.element.RootLocationElement;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

public class SitemapGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SitemapGenerator.class);

    public void generate() throws CouldNotPerformException {
        logger.info("generate sitemap...");
        try {
            serializeToFile(new BcoSitemapBuilder().append(new RootLocationElement()).build());
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Could not generate sitemap!", ex);
        }
    }

    private void serializeToFile(final String content) throws CouldNotPerformException {
        try {
            final File configFile = JPService.getProperty(JPOpenHABSitemap.class).getValue();
            FileUtils.writeStringToFile(configFile, content, Charset.forName("UTF8"), false);
            logger.info("Sitemap[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize sitemap to file!", ex);
        }
    }
}
