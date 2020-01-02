package org.openbase.bco.dal.control.layer.unit.device;

/*
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import org.openbase.bco.dal.lib.jp.JPBenchmarkMode;
import org.openbase.bco.dal.lib.jp.JPHardwareSimulationMode;
import org.openbase.bco.dal.lib.jp.JPProviderControlMode;
import org.openbase.bco.dal.lib.layer.unit.device.DeviceManager;
import org.openbase.bco.authentication.lib.BCO;
import org.openbase.jul.pattern.launch.AbstractLauncher;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPPrefix;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class DeviceManagerLauncher extends AbstractLauncher<DeviceManagerImpl> {

    public DeviceManagerLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(DeviceManager.class, DeviceManagerImpl.class);
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPPrefix.class);
        JPService.registerProperty(JPHardwareSimulationMode.class);
        JPService.registerProperty(JPBenchmarkMode.class);
        JPService.registerProperty(JPProviderControlMode.class);
    }

    public static void main(String[] args) throws Throwable {
        BCO.printLogo();
        main(args, DeviceManager.class, DeviceManagerLauncher.class);
    }
}

