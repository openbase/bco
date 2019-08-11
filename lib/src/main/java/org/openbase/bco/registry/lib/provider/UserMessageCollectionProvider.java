package org.openbase.bco.registry.lib.provider;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.jul.annotation.RPCMethod;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.type.domotic.communication.UserMessageType.UserMessage;
import org.openbase.type.domotic.communication.UserMessageType.UserMessage.MessageType;

import java.util.List;

public interface UserMessageCollectionProvider {

    /**
     * Method returns all registered user messages.
     *
     * @return the user messages stored in this registry.
     *
     * @throws CouldNotPerformException is thrown if the request fails.
     */
    List<UserMessage> getUserMessages() throws CouldNotPerformException;

    /**
     * Method returns true if the user message with the given id is
     * registered, otherwise false.
     *
     * @param userMessageId the id to identify the message.
     *
     * @return true if the message exists.
     */
    @RPCMethod
    Boolean containsUserMessageById(final String userMessageId);

    /**
     * Method returns the user message which is registered with the given
     * message id.
     *
     * @param userMessageId
     *
     * @return the requested user message.
     *
     * @throws NotAvailableException is thrown if the request fails.
     */
    @RPCMethod
    UserMessage getUserMessageById(final String userMessageId) throws NotAvailableException;

    /**
     * Method returns the user message which is registered with the given
     * message id. Additionally the type will be verified.
     *
     * @param userMessageId the identifier of the unit.
     * @param messageType   the type to verify.
     *
     * @return the requested user message validated with the given message type.
     *
     * @throws NotAvailableException is thrown if the request fails.
     */
    default UserMessage getUserMessageByIdAndMessageType(final String userMessageId, final MessageType messageType) throws NotAvailableException {
        final UserMessage userMessage = getUserMessageById(userMessageId);

        try {
            // validate type
            if (messageType != MessageType.UNKNOWN && userMessage.getMessageType() != messageType) {
                throw new VerificationFailedException("Referred Message[" + userMessageId + "] is not compatible to given type!");
            }
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UserMessageId", userMessageId, ex);
        }

        return userMessage;
    }

    /**
     * Method returns a list of all globally registered messages of the given {@code type}.
     * <p>
     * Note: The type {@code MessageType.UNKNOWN} is used as wildcard and will return a list of all registered messages.
     *
     * @param type the message type to filter.
     *
     * @return a list of user messages.
     *
     * @throws CouldNotPerformException is thrown in case something goes wrong during the request.
     */
    List<UserMessage> getUserMessagesByMessageType(final MessageType type) throws CouldNotPerformException;
}
