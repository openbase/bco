/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.registry.app.core.consistency;

/*
 * #%L
 * BCO Registry App Core
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import rst.homeautomation.control.app.AppClassType.AppClass;
import rst.homeautomation.control.app.AppConfigType.AppConfig;
import rst.homeautomation.control.app.AppRegistryDataType.AppRegistryData;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.ProtoBufRegistry;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class AppConfigAppClassIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, AppConfig, AppConfig.Builder> {

    private final ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, AppRegistryData.Builder> appClassRegistry;

    public AppConfigAppClassIdConsistencyHandler(final ProtoBufFileSynchronizedRegistry<String, AppClass, AppClass.Builder, AppRegistryData.Builder> appClassRegistry) {
        this.appClassRegistry = appClassRegistry;
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, AppConfig, AppConfig.Builder> entry, final ProtoBufMessageMap<String, AppConfig, AppConfig.Builder> entryMap, final ProtoBufRegistry<String, AppConfig, AppConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AppConfig appConfig = entry.getMessage();

        if (!appConfig.hasAppClassId() || appConfig.getAppClassId().isEmpty()) {
            throw new NotAvailableException("appclass.id");
        }

        // get throws a CouldNotPerformException if the agent class with the id does not exists
        appClassRegistry.get(appConfig.getAppClassId());
    }
}
