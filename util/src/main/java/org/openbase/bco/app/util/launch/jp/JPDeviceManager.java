package org.openbase.bco.app.util.launch.jp;

/*-
 * #%L
 * BCO App Utility
 * %%
 * Copyright (C) 2018 - 2020 openbase.org
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

import org.openbase.bco.app.util.launch.jp.JPDeviceManager.BuildinDeviceManager;
import org.openbase.bco.device.openhab.OpenHABDeviceManagerLauncher;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.AbstractJPEnum;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jul.processing.StringProcessor;

import java.util.Arrays;

public class JPDeviceManager extends AbstractJPEnum<BuildinDeviceManager> {

    public final static String[] COMMAND_IDENTIFIERS = {"--device-manager"};

    public enum BuildinDeviceManager {

        NON(null),
        OPENHAB(OpenHABDeviceManagerLauncher.class);

        public final Class<? extends AbstractLauncher<?>> launcher;

        BuildinDeviceManager(final Class<? extends AbstractLauncher<?>> launcher) {
            this.launcher = launcher;
        }
    }

    @Override
    protected BuildinDeviceManager getPropertyDefaultValue() throws JPNotAvailableException {

        return BuildinDeviceManager.NON;
    }

    public JPDeviceManager() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public String getDescription() {
        return "Can be used to declare a list of device mangers (e.g.: "+ StringProcessor.transformCollectionToString(Arrays.asList(BuildinDeviceManager.values()), buildinDeviceManager -> buildinDeviceManager.name(), ", ") +") to launch within this bco instance.";
    }
}
