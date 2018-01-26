package org.openbase.bco.registry.user.activity.core.plugin;

/*-
 * #%L
 * BCO Registry User Activity Core
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
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.storage.registry.FileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.jul.storage.registry.ProtoBufRegistry;
import org.openbase.jul.storage.registry.Registry;
import org.openbase.jul.storage.registry.plugin.FileRegistryPluginAdapter;
import rst.domotic.activity.UserActivityClassType.UserActivityClass;
import rst.domotic.activity.UserActivityClassType.UserActivityClass.Builder;
import rst.domotic.activity.UserActivityClassType.UserActivityClass.UserActivityType;
import rst.domotic.registry.UserActivityRegistryDataType.UserActivityRegistryData;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UserActivityClassCreatorRegistryPlugin extends FileRegistryPluginAdapter<String, IdentifiableMessage<String, UserActivityClass, UserActivityClass.Builder>, ProtoBufRegistry<String, UserActivityClass, UserActivityClass.Builder>> {

    private final ProtoBufRegistry<String, UserActivityClass, UserActivityClass.Builder> userActivityClassRegistry;

    public UserActivityClassCreatorRegistryPlugin(ProtoBufRegistry<String, UserActivityClass, Builder> userActivityClassRegistry) {
        // todo remove and use given registry
        this.userActivityClassRegistry = userActivityClassRegistry;
    }

    @Override
    public void init(final ProtoBufRegistry<String, UserActivityClass, UserActivityClass.Builder> registry) throws InitializationException, InterruptedException {
        try {
            UserActivityClass userActivityClass;

            // create missing unit template
            if (userActivityClassRegistry.size() <= UserActivityType.values().length - 1) {
                for (UserActivityType userActivityType : UserActivityType.values()) {
                    if (userActivityType == UserActivityType.UNKNOWN) {
                        continue;
                    }
                    userActivityClass = UserActivityClass.newBuilder().setType(userActivityType).setLabel(StringProcessor.transformUpperCaseToCamelCase(userActivityType.name())).build();
                    if (!containsUnitTemplateByType(userActivityType)) {
                        userActivityClassRegistry.register(userActivityClass);
                    }
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException("Could not init " + getClass().getSimpleName() + "!", ex);
        }
    }

    private boolean containsUnitTemplateByType(UserActivityType type) throws CouldNotPerformException {
        for (UserActivityClass userActivityClass : userActivityClassRegistry.getMessages()) {
            if (userActivityClass.getType() == type) {
                return true;
            }
        }
        return false;
    }

}
