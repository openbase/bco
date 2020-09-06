
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

import org.openbase.bco.device.openhab.jp.JPOpenHABSitemap;
import org.openbase.bco.authentication.lib.BCO;
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
    }

    /**
     * @param args the command line arguments
     * @throws InterruptedException
     * @throws CouldNotPerformException
     */
    public static void main(final String[] args) throws InterruptedException, CouldNotPerformException {
        BCO.printLogo();
        AbstractLauncher.main(BCO.class, SitemapSynchronizer.class, args, OpenHABSitemapSynchronizerLauncher.class);
    }
}
