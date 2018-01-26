package org.openbase.bco.registry.unit.core.plugin;

/*-
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
import java.util.HashSet;
import java.util.Set;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import org.openbase.jul.storage.registry.plugin.ProtobufRegistryPluginAdapter;
import rst.domotic.registry.UnitRegistryDataType.UnitRegistryData;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class AuthorizationGroupCreationPlugin extends ProtobufRegistryPluginAdapter<String, UnitConfig, Builder> {

    public static final String ADMIN_GROUP_LABEL = "Admin";
    public static final String BCO_GROUP_LABEL = "BCO";

    private final ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry;
    private final Set<String> labelSet;

    public AuthorizationGroupCreationPlugin(ProtoBufFileSynchronizedRegistry<String, UnitConfig, UnitConfig.Builder, UnitRegistryData.Builder> authorizationGroupRegistry) {
        this.authorizationGroupRegistry = authorizationGroupRegistry;

        this.labelSet = new HashSet<>();
        this.labelSet.add(ADMIN_GROUP_LABEL);
        this.labelSet.add(BCO_GROUP_LABEL);
    }

    @Override
    public void init(ProtoBufRegistry<String, UnitConfig, Builder> registry) throws InitializationException, InterruptedException {
        try {
            UnitConfig.Builder authorizationGoupUnitConfig = UnitConfig.newBuilder();
            authorizationGoupUnitConfig.setType(UnitType.AUTHORIZATION_GROUP);

            // create missing authorization groups
            for (String label : this.labelSet) {
                if (!containsAuthorizationGroupByLabel(label)) {
                    authorizationGoupUnitConfig.setLabel(label);
                    this.authorizationGroupRegistry.register(authorizationGoupUnitConfig.build());
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }

    private boolean containsAuthorizationGroupByLabel(String label) throws CouldNotPerformException {
        for (UnitConfig unitConfig : authorizationGroupRegistry.getMessages()) {
            if (unitConfig.getLabel().equals(label)) {
                return true;
            }
        }
        return false;
    }

}
