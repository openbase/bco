package org.openbase.bco.registry.unit.core.plugin;

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

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.openbase.type.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Locale;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RequiredAuthorizationGroupCreationPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UnitConfig, UnitConfig.Builder>, ProtoBufRegistry<String, UnitConfig, Builder>> {
    
    public static final String ADMIN_GROUP_LABEL = "Admin";

    private final ProtoBufRegistry<String, UnitConfig, Builder> authorisationGroupRegistry;

    public RequiredAuthorizationGroupCreationPlugin(ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorisationGroupRegistry) {
        this.authorisationGroupRegistry = authorisationGroupRegistry;
    }

    @Override
    public void init(ProtoBufRegistry<String, UnitConfig, Builder> config) throws InitializationException, InterruptedException {
        try {
            if(!containsAuthorizationGroupByLabel(ADMIN_GROUP_LABEL)) {
                UnitConfig.Builder unitConfig = UnitConfig.newBuilder().setUnitType(UnitType.AUTHORIZATION_GROUP);
                LabelProcessor.addLabel(unitConfig.getLabelBuilder(), Locale.ENGLISH, ADMIN_GROUP_LABEL);
                authorisationGroupRegistry.register(unitConfig.build());
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    private boolean containsAuthorizationGroupByLabel(final String label) throws CouldNotPerformException {
        for (UnitConfig authorizationGroupUnitConfig : authorisationGroupRegistry.getMessages()) {
            if (authorizationGroupUnitConfig.getLabel().equals(label)) {
                return true;
            }
        }
        return false;
    }
}
