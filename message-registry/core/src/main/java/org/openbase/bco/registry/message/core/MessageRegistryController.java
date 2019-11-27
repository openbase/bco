package org.openbase.bco.registry.message.core;

/*
 * #%L
 * BCO Registry Message Core
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

import com.google.protobuf.Descriptors.FieldDescriptor;
import org.openbase.bco.authentication.lib.AuthenticatedServiceProcessor;
import org.openbase.bco.authentication.lib.AuthorizationHelper;
import org.openbase.bco.authentication.lib.AuthorizationHelper.PermissionType;
import org.openbase.bco.registry.lib.com.AbstractRegistryController;
import org.openbase.bco.registry.lib.jp.JPBCODatabaseDirectory;
import org.openbase.bco.registry.message.lib.MessageRegistry;
import org.openbase.bco.registry.message.lib.generator.UserMessageIdGenerator;
import org.openbase.bco.registry.message.lib.jp.JPMessageRegistryScope;
import org.openbase.bco.registry.message.lib.jp.JPUserMessageDatabaseDirectory;
import org.openbase.bco.registry.unit.lib.auth.AuthorizationWithTokenHelper;
import org.openbase.bco.registry.unit.remote.CachedUnitRegistryRemote;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPServiceException;
import org.openbase.jul.communication.controller.RPCHelper;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.InvalidStateException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.iface.RSBLocalServer;
import org.openbase.jul.pattern.ListFilter;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.storage.registry.ProtoBufFileSynchronizedRegistry;
import org.openbase.type.domotic.authentication.AuthenticatedValueType.AuthenticatedValue;
import org.openbase.type.domotic.authentication.UserClientPairType.UserClientPair;
import org.openbase.type.domotic.communication.UserMessageType.UserMessage;
import org.openbase.type.domotic.registry.MessageRegistryDataType.MessageRegistryData;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class MessageRegistryController extends AbstractRegistryController<MessageRegistryData, MessageRegistryData.Builder> implements MessageRegistry {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(MessageRegistryData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(UserMessage.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AuthenticatedValue.getDefaultInstance()));
    }

    public final static UserMessageIdGenerator USER_MESSAGE_ID_GENERATOR = new UserMessageIdGenerator();

    private final static org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MessageRegistryController.class);

    private final ProtoBufFileSynchronizedRegistry<String, UserMessage, UserMessage.Builder, MessageRegistryData.Builder> userMessageRegistry;

    public MessageRegistryController() throws InstantiationException, InterruptedException {
        super(JPMessageRegistryScope.class, MessageRegistryData.newBuilder());
        try {
            // verify that database exists and fail if not so no further errors are printed because they are based on this property.
            try {
                LOGGER.info("Use bco registry at " + JPService.getProperty(JPBCODatabaseDirectory.class).getValue());
            } catch (JPServiceException ex) {
                throw new NotAvailableException("Database");
            }

            this.userMessageRegistry = new ProtoBufFileSynchronizedRegistry<>(UserMessage.class, getBuilderSetup(), getDataFieldDescriptor(MessageRegistryData.USER_MESSAGE_FIELD_NUMBER), USER_MESSAGE_ID_GENERATOR, JPService.getProperty(JPUserMessageDatabaseDirectory.class).getValue(), protoBufJSonFileProvider);
        } catch (JPServiceException | NullPointerException | CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerRegistries() {
        registerRegistry(userMessageRegistry);
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerConsistencyHandler() throws CouldNotPerformException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    protected void registerPlugins() throws CouldNotPerformException, InterruptedException {
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected void registerDependencies() throws CouldNotPerformException {
        registerDependency(CachedUnitRegistryRemote.getRegistry().getUnitConfigRemoteRegistry(false), UserMessage.class);
    }

    @Override
    public final void syncRegistryFlags() throws CouldNotPerformException {
        setDataField(MessageRegistryData.USER_MESSAGE_REGISTRY_READ_ONLY_FIELD_NUMBER, userMessageRegistry.isReadOnly());
        setDataField(MessageRegistryData.USER_MESSAGE_REGISTRY_CONSISTENT_FIELD_NUMBER, userMessageRegistry.isConsistent());
    }

    @Override
    public void registerMethods(final RSBLocalServer server) throws CouldNotPerformException {
        super.registerMethods(server);
        RPCHelper.registerInterface(MessageRegistry.class, this, server);
    }

    @Override
    public void notifyChange() throws CouldNotPerformException, InterruptedException {
        updateTransactionId();
        super.notifyChange();
    }

    @Override
    public Future<UserMessage> registerUserMessage(final UserMessage userMessage) {
        return GlobalCachedExecutorService.submit(() -> {
            UserMessage result = userMessageRegistry.register(userMessage);
            return result;
        });
    }

    @Override
    public Future<AuthenticatedValue> registerUserMessageAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, UserMessage.class, this, (userMessage, authenticationBaseData) -> {

                    // disable check which seems not be required when submitting messages
                    //AuthorizationWithTokenHelper.canDo(authenticationBaseData, location, PermissionType.WRITE, this);

                    // register the new user message
                    return userMessageRegistry.register(userMessage);
                }
        ));
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public List<UserMessage> getUserMessages() throws CouldNotPerformException {
        return userMessageRegistry.getMessages();
    }

    @Override
    public UserMessage getUserMessageById(final String userMessageId) throws NotAvailableException {
        for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
            // filter to avoid useless and heavy lookups
            if (!registry.contains(userMessageId)) {
                continue;
            }
            try {
                return (UserMessage) registry.getMessage(userMessageId);
            } catch (CouldNotPerformException ex) {
                throw new NotAvailableException("UserMessageId", userMessageId, new CouldNotPerformException("Lookup via " + registry.getName() + " of id [" + userMessageId + "] failed!", ex));
            }
        }
        throw new NotAvailableException("UserMessageId", userMessageId, new CouldNotPerformException("None of the unit registries contains an entry with the id [" + userMessageId + "]"));
    }

    @Override
    public Boolean containsUserMessageById(final String userMessageId) {
        for (ProtoBufFileSynchronizedRegistry registry : getRegistries()) {
            if (registry.contains(userMessageId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean containsUserMessage(final UserMessage userMessage) {
        return userMessageRegistry.contains(userMessage);
    }

    @Override
    public Future<UserMessage> updateUserMessage(final UserMessage userMessage) {
        return GlobalCachedExecutorService.submit(() -> {
            UserMessage result = userMessageRegistry.update(userMessage);
            return result;
        });
    }

    @Override
    public Future<AuthenticatedValue> updateUserMessageAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, UserMessage.class, this,
                (userMessage, authenticationBaseData) -> {
                    // verify write permissions for the old and new user message
                    final UserMessage old = userMessageRegistry.getMessage(userMessage.getId());
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, old, PermissionType.WRITE, CachedUnitRegistryRemote.getRegistry());
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, userMessage, PermissionType.WRITE, CachedUnitRegistryRemote.getRegistry());
                    return userMessageRegistry.update(userMessage);
                }
        ));
    }

    @Override
    public Future<UserMessage> removeUserMessage(final UserMessage userMessage) {
        return GlobalCachedExecutorService.submit(() -> {
            UserMessage result = userMessageRegistry.remove(userMessage);
            return result;
        });
    }

    @Override
    public Future<AuthenticatedValue> removeUserMessageAuthenticated(final AuthenticatedValue authenticatedValue) {
        return GlobalCachedExecutorService.submit(() -> AuthenticatedServiceProcessor.authenticatedAction(authenticatedValue, UserMessage.class, this,
                (userMessage, authenticationBaseData) -> {
                    // verify write permissions for the old user message
                    final UserMessage old = userMessageRegistry.getMessage(userMessage.getId());
                    AuthorizationWithTokenHelper.canDo(authenticationBaseData, old, PermissionType.WRITE, CachedUnitRegistryRemote.getRegistry());
                    return userMessageRegistry.remove(userMessage);
                }
        ));
    }

    public Boolean isUserMessageRegistryReadOnly() {
        return userMessageRegistry.isReadOnly();
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Boolean isUserMessageRegistryConsistent() {
        return userMessageRegistry.isConsistent();
    }

    @Override
    protected void registerRemoteRegistries() {
    }

    @Override
    public void validateData() throws InvalidStateException {
        if (!isDataAvailable()) {
            throw new InvalidStateException(this + " not synchronized yet!", new NotAvailableException("data"));
        }
    }

    @Override
    protected MessageRegistryData filterDataForUser(final MessageRegistryData.Builder dataBuilder, final UserClientPair userClientPair) throws CouldNotPerformException {
        // Create a filter which removes all user messages from a list without read permissions to its location by the user
        final ListFilter<UserMessage> readFilter = userMessage -> {
            try {
                boolean senderPermission = AuthorizationHelper.canRead(CachedUnitRegistryRemote.getRegistry().getUnitConfigById(userMessage.getSenderId()), userClientPair, CachedUnitRegistryRemote.getRegistry().getAuthorizationGroupUnitConfigRemoteRegistry(true).getEntryMap(), CachedUnitRegistryRemote.getRegistry().getLocationUnitConfigRemoteRegistry(true).getEntryMap());
                boolean receiverPermission = AuthorizationHelper.canRead(CachedUnitRegistryRemote.getRegistry().getUnitConfigById(userMessage.getRecipientId()), userClientPair, CachedUnitRegistryRemote.getRegistry().getAuthorizationGroupUnitConfigRemoteRegistry(true).getEntryMap(), CachedUnitRegistryRemote.getRegistry().getLocationUnitConfigRemoteRegistry(true).getEntryMap());
                return !(senderPermission || receiverPermission);
            } catch (CouldNotPerformException e) {
                // if id could not resolved, than we filter the element.
                return true;
            }
        };
        // iterate over all fields of unit registry data
        for (FieldDescriptor fieldDescriptor : dataBuilder.getAllFields().keySet()) {
            // only filter repeated fields
            if (!fieldDescriptor.isRepeated()) {
                continue;
            }

            // only filter fields of type UserMessage
            if (!fieldDescriptor.getMessageType().getName().equals(UserMessage.getDescriptor().getName())) {
                continue;
            }

            // copy list, filter it and set as new list for the field
            dataBuilder.setField(fieldDescriptor, readFilter.filter(new ArrayList<>((List<UserMessage>) dataBuilder.getField(fieldDescriptor))));
        }

        return dataBuilder.build();
    }
}
