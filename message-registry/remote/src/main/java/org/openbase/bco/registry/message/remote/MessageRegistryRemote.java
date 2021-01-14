package org.openbase.bco.registry.message.remote;

/*
 * #%L
 * BCO Registry Message Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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
 */

import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.bco.registry.lib.com.AbstractRegistryRemote;
import org.openbase.bco.registry.lib.com.SynchronizedRemoteRegistry;
import org.openbase.bco.registry.message.lib.MessageRegistry;
import org.openbase.bco.registry.message.lib.jp.JPMessageRegistryScope;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.preset.JPReadOnly;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.util.TransactionSynchronizationFuture;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.communication.UserMessageType.UserMessage;
import org.openbase.type.domotic.registry.MessageRegistryDataType.MessageRegistryData;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class MessageRegistryRemote extends AbstractRegistryRemote<MessageRegistryData> implements MessageRegistry, RegistryRemote<MessageRegistryData> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MessageRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserMessage.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }

    private final SynchronizedRemoteRegistry<String, UserMessage, UserMessage.Builder> userMessageRemoteRegistry;

    public MessageRegistryRemote() throws InstantiationException {
        super(JPMessageRegistryScope.class, MessageRegistryData.class);
        try {
            this.userMessageRemoteRegistry = new SynchronizedRemoteRegistry<>(this.getInternalPrioritizedDataObservable(), this, MessageRegistryData.USER_MESSAGE_FIELD_NUMBER);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }


    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        CachedUnitRegistryRemote.getRegistry().waitForData();
        super.waitForData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerRemoteRegistries() {
        /* ATTENTION: the order here is important, if somebody registers an observer
         * on one of these remote registries and tries to get values from other remote registries
         * which are registered later than these are not synced yet
         */
        registerRemoteRegistry(userMessageRemoteRegistry);
    }

    public SynchronizedRemoteRegistry<String, UserMessage, UserMessage.Builder> getUserMessageRemoteRegistry() throws NotAvailableException {
        try {
            validateData();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UserMessageRemoteRegistry", ex);
        }
        return userMessageRemoteRegistry;
    }
    /**
     * {@inheritDoc}
     *
     * @param userMessage {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<UserMessage> registerUserMessage(final UserMessage userMessage) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(userMessage, UserMessage.class, SessionManager.getInstance(), authenticatedValue -> registerUserMessageAuthenticated(authenticatedValue));
    }

    @Override
    public Future<AuthenticatedValue> registerUserMessageAuthenticated(AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    /**
     * {@inheritDoc}
     *
     * @param userMessageId {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws org.openbase.jul.exception.NotAvailableException {@inheritDoc}
     */
    @Override
    public UserMessage getUserMessageById(final String userMessageId) throws NotAvailableException {
        try {
            validateData();
            return userMessageRemoteRegistry.getMessage(userMessageId);
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UserMessageId", userMessageId, ex);
        }
    }

    @Override
    public List<UserMessage> getUserMessages() throws CouldNotPerformException {
        try {
            validateData();
            return userMessageRemoteRegistry.getMessages();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("UserMessages", ex);
        }
    }

    @Override
    public Boolean containsUserMessage(final UserMessage userMessage) {
        try {
            validateData();
            return userMessageRemoteRegistry.contains(userMessage);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean containsUserMessageById(final String userMessageId) {
        try {
            validateData();
            return userMessageRemoteRegistry.contains(userMessageId);
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Future<UserMessage> updateUserMessage(final UserMessage userMessage) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(userMessage, UserMessage.class, SessionManager.getInstance(), authenticatedValue -> updateUserMessageAuthenticated(authenticatedValue));
    }

    @Override
    public Future<AuthenticatedValue> updateUserMessageAuthenticated(final AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    @Override
    public Future<UserMessage> removeUserMessage(final UserMessage userMessage) {
        return AuthenticatedServiceProcessor.requestAuthenticatedAction(userMessage, UserMessage.class, SessionManager.getInstance(), authenticatedValue -> removeUserMessageAuthenticated(authenticatedValue));
    }

    @Override
    public Future<AuthenticatedValue> removeUserMessageAuthenticated(final AuthenticatedValue authenticatedValue) {
        return new TransactionSynchronizationFuture<>(RPCHelper.callRemoteMethod(authenticatedValue, this, AuthenticatedValue.class), this);
    }

    @Override
    public Boolean isUserMessageRegistryReadOnly() {
        if (JPService.getValue(JPReadOnly.class, false) || !isConnected()) {
            return true;
        }
        try {
            validateData();
            return getData().getUserMessageRegistryReadOnly();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Boolean isUserMessageRegistryConsistent() {
        try {
            validateData();
            return getData().getUserMessageRegistryConsistent();
        } catch (InvalidStateException e) {
            return true;
        }
    }

    @Override
    public Boolean isConsistent() {
        return isUserMessageRegistryConsistent();
    }
}
