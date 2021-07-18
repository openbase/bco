package org.openbase.bco.registry.unit.core.consistency.userconfig;

/*-
 * #%L
 * BCO Registry Unit Core
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
 */

import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jps.preset.JPLocale;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.container.ProtoBufMessageMap;
import org.openbase.jul.storage.registry.AbstractProtoBufRegistryConsistencyHandler;
import org.openbase.jul.storage.registry.EntryModification;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.jp.JPRecoverDB;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Consistency handler making sure that users always have a valid language defined.
 * If no language is defined the property {@link JPLocale} is considered.
 * A language is valid if it is a valid iso 3 language code.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserConfigLanguageConsistencyHandler extends AbstractProtoBufRegistryConsistencyHandler<String, UnitConfig, Builder> {

    private final List<String> isoLanguageList = Arrays.asList(Locale.getISOLanguages());

    @Override
    public void processData(final String id, final IdentifiableMessage<String, UnitConfig, Builder> entry, final ProtoBufMessageMap<String, UnitConfig, Builder> entryMap, final ProtoBufRegistry<String, UnitConfig, Builder> registry) throws CouldNotPerformException, EntryModification {
        final UnitConfig.Builder unitConfig = entry.getMessage().toBuilder();

        try {
            if (!unitConfig.getUserConfig().hasLanguage() || unitConfig.getUserConfig().getLanguage().isEmpty()) {
                final String language = JPService.getProperty(JPLocale.class).getValue().getLanguage();
                unitConfig.getUserConfigBuilder().setLanguage(language);
                throw new EntryModification(entry.setMessage(unitConfig, this), this);
            }

            if (!isoLanguageList.contains(unitConfig.getUserConfig().getLanguage())) {
                if (JPService.getProperty(JPRecoverDB.class).getValue()) {
                    final String language = JPService.getProperty(JPLocale.class).getValue().getLanguage();
                    unitConfig.getUserConfigBuilder().setLanguage(language);
                    throw new EntryModification(entry.setMessage(unitConfig, this), this);
                } else {
                    throw new InvalidStateException("Language [" + unitConfig.getUserConfig().getLanguage() + "] of user [" + unitConfig.getUserConfig().getUserName() + "] is not a valid ISO3 languageCode!");
                }

            }
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not check db recovery or locale property", ex);
        }
    }
}
