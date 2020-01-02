package org.openbase.bco.registry.message.lib;

/*
 * #%L
 * BCO Registry Message Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */

import org.openbase.bco.registry.lib.provider.UserMessageCollectionProvider;
import org.openbase.bco.registry.template.remote.CachedTemplateRegistryRemote;
import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.iface.Shutdownable;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.storage.registry.RegistryService;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.communication.UserMessageType.UserMessage;
import org.openbase.type.domotic.communication.UserMessageType.UserMessage.MessageType;
import org.openbase.type.domotic.registry.MessageRegistryDataType.MessageRegistryData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface MessageRegistry extends DataProvider<MessageRegistryData>, UserMessageCollectionProvider, Shutdownable, RegistryService {

    /**
     * This method registers the given user message in the registry.
     * Future get canceled if the entry already exists or results in an inconsistent registry
     *
     * @param userMessage the user message to register.
     *
     * @return the registered user message with all applied consistency changes.
     */
    @RPCMethod
    Future<UserMessage> registerUserMessage(final UserMessage userMessage);

    @RPCMethod
    Future<AuthenticatedValue> registerUserMessageAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method updates the given user message.
     *
     * @param userMessage the updated user message.
     *
     * @return the updated user message with all applied consistency changes.
     */
    @RPCMethod
    Future<UserMessage> updateUserMessage(final UserMessage userMessage);

    @RPCMethod
    Future<AuthenticatedValue> updateUserMessageAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method removes the given user message out of the global registry.
     *
     * @param userMessage the user message to remove.
     *
     * @return The removed user message.
     */
    @RPCMethod
    Future<UserMessage> removeUserMessage(final UserMessage userMessage);

    @RPCMethod
    Future<AuthenticatedValue> removeUserMessageAuthenticated(final AuthenticatedValue authenticatedValue);

    /**
     * Method returns true if the user message with the given id is
     * registered, otherwise false. The user message id field is used for the
     * comparison.
     *
     * @param userMessage the user message used for the identification.
     *
     * @return true if the unit exists or false if the entry does not exists or the registry is not available.
     */
    @RPCMethod
    Boolean containsUserMessage(final UserMessage userMessage);

    /**
     * Method returns true if the underling registry is marked as read only. A
     * registry is marked as read only in case of inconsistently data entries or
     * if the underling database is loaded out of a version tag.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if read only.
     */
    @RPCMethod
    Boolean isUserMessageRegistryReadOnly();

    /**
     * Method returns a list of all globally registered messages of the given {@code type}.
     * <p>
     * Note: The type {@code MessageType.UNKNOWN} is used as wildcard and will return a list of all registered messages.
     *
     * @param messageType the message type to filter.
     *
     * @return a list of user messages.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    default List<UserMessage> getUserMessagesByMessageType(final MessageType messageType) throws CouldNotPerformException {
        validateData();
        List<UserMessage> userMessages = new ArrayList<>();
        for (UserMessage userMessage : getUserMessages()) {
            if (messageType == MessageType.UNKNOWN || userMessage.getMessageType() == messageType) {
                userMessages.add(userMessage);
            }
        }
        return userMessages;
    }

    /**
     * Method returns true if the underling registry is marked as consistent.
     * <p>
     * Note: Method returns true in case the registry is not available. Maybe you need to check this in advance.
     *
     * @return true if consistent
     */
    @RPCMethod
    Boolean isUserMessageRegistryConsistent();
}
