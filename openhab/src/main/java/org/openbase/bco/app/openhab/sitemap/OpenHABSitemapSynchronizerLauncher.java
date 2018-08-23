
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

import org.openbase.bco.app.openhab.jp.JPOpenHABConfiguration;
import org.openbase.bco.app.openhab.jp.JPOpenHABDistribution;
import org.openbase.bco.app.openhab.jp.JPOpenHABSitemap;
import org.openbase.bco.registry.lib.BCO;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPPrefix;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.launch.AbstractLauncher;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class OpenHABSitemapSynchronizerLauncher extends AbstractLauncher<SitemapSynchronizer> {

    public OpenHABSitemapSynchronizerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(OpenHABSitemapSynchronizerLauncher.class, SitemapSynchronizer.class);
    }

    @Override
    protected void loadProperties() {
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPOpenHABSitemap.class);
        JPService.registerProperty(JPOpenHABDistribution.class);
        JPService.registerProperty(JPOpenHABConfiguration.class);
    }

    /**
     * @param args the command line arguments
     * @throws InterruptedException
     * @throws CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(args, SitemapSynchronizer.class, OpenHABSitemapSynchronizerLauncher.class);
    }
}
