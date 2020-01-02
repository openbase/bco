package org.openbase.bco.device.openhab.sitemap;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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
import org.openbase.bco.device.openhab.jp.JPOpenHABConfiguration;
import org.openbase.bco.device.openhab.jp.JPOpenHABSitemap;
import org.openbase.bco.device.openhab.sitemap.element.RootLocationElement;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.location.LocationConfigType.LocationConfig.LocationType;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;

public class SitemapGenerator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SitemapGenerator.class);


    public void generate() throws CouldNotPerformException {
        generate(Registries.getUnitRegistry().getRootLocationConfig());
    }

    public void generate(final UnitConfig zone) throws CouldNotPerformException {
        logger.info("generate sitemap...");
        try {

            // generate filename
            final String fileName = "bco"+zone.getAlias(0).toLowerCase().replace("-", "");

            // generate for current zone
            serializeToFile(new BcoSitemapBuilder(zone, fileName).append(new RootLocationElement(zone)).build(), fileName);

            // generate for child zones
            try {
                for (String childId : zone.getLocationConfig().getChildIdList()) {
                    final UnitConfig childLocationConfig = Registries.getUnitRegistry().getUnitConfigById(childId);
                    if(childLocationConfig.getLocationConfig().getLocationType() == LocationType.ZONE) {
                        generate(childLocationConfig);
                    }
                }
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }
        } catch (NullPointerException ex) {
            throw new CouldNotPerformException("Could not generate sitemap!", ex);
        }
    }

    private void serializeToFile(final String content, final String fileName) throws CouldNotPerformException {
        try {
            final File configFile = new File(JPService.getProperty(JPOpenHABSitemap.class).getValue(), fileName+".sitemap");
            FileUtils.writeStringToFile(configFile, content, Charset.forName("UTF8"), false);
            logger.info("Sitemap[" + configFile.getAbsolutePath() + "] successfully generated.");
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not serialize sitemap to file!", ex);
        }
    }
}
