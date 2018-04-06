package org.openbase.bco.registry.unit.core.consistency.appconfig;

/*
 * #%L
 * BCO Registry Unit Core
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.*;
import rst.domotic.unit.app.AppClassType.AppClass;
import rst.domotic.unit.app.AppClassType.AppClass.Builder;
import rst.domotic.unit.app.AppConfigType.AppConfig;
import rst.domotic.registry.AppRegistryDataType.AppRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AppConfigAppClassIdConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, UnitConfig.Builder> {

    private final Registry<String, IdentifiableMessage<String, AppClass, Builder>> appClassRegistry;

    public AppConfigAppClassIdConsistencyHandler(final Registry<String, IdentifiableMessage<String, AppClass, Builder>> appClassRegistry) {
        this.appClassRegistry = appClassRegistry;
    }

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, UnitConfig.Builder> entry, final ProtoBufMessageMap<String, UnitConfig, UnitConfig.Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, UnitConfig.Builder> registry) throws CouldNotPerformException, EntryModification {
        AppConfig appConfig = entry.getMessage().getAppConfig();

        if (!appConfig.hasAppClassId() || appConfig.getAppClassId().isEmpty()) {
            throw new NotAvailableException("appclass.id");
        }

        // get throws a CouldNotPerformException if the agent class with the id does not exists
        appClassRegistry.get(appConfig.getAppClassId());
    }
}
