package org.openbase.bco.registry.message.core;

/*
 * #%L
 * BCO Registry Message Core
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
 *
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */

import org.openbase.bco.authentication.lib.BCO;
import org.openbase.bco.registry.lib.launch.AbstractRegistryLauncher;
import org.openbase.bco.registry.message.lib.MessageRegistry;
import org.openbase.bco.registry.message.lib.jp.JPMessageRegistryScope;
import org.openbase.bco.registry.message.lib.jp.JPUserMessageDatabaseDirectory;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPDebugMode;
import org.openbase.jps.preset.JPForce;
import org.openbase.jps.preset.JPLocale;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.communication.jp.JPComHost;
import org.openbase.jul.communication.jp.JPComPort;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;

public class MessageRegistryLauncher extends AbstractRegistryLauncher<MessageRegistryController> {

    public MessageRegistryLauncher() throws org.openbase.jul.exception.InstantiationException {
        super(MessageRegistry.class, MessageRegistryController.class);
    }

    public static void main(String[] args) throws Throwable {
        BCO.printLogo();
        main(BCO.class, MessageRegistry.class, args, MessageRegistryLauncher.class);
    }

    @Override
    public boolean isCoreLauncher() {
        return false;
    }

    @Override
    public void loadProperties() {
        JPService.registerProperty(JPMessageRegistryScope.class);
        JPService.registerProperty(JPReadOnly.class);
        JPService.registerProperty(JPForce.class);
        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPRecoverDB.class);
        JPService.registerProperty(JPUserMessageDatabaseDirectory.class);
        JPService.registerProperty(JPLocale.class);

        JPService.registerProperty(JPComHost.class);
        JPService.registerProperty(JPComPort.class);
    }
}
