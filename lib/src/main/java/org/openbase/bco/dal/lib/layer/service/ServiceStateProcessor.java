package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
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
 */

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.timing.TimestampType.Timestamp;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class ServiceStateProcessor {

    public static final String INTERNAL_MAP_CLASS_NAME = "$MapFieldEntry";
    public static final String FIELD_NAME_LAST_VALUE_OCCURRENCE = "last_value_occurrence";
    public static final String FIELD_NAME_TIMESTAMP = "timestamp";
    public static final String FIELD_NAME_KEY = "key";
    public static final String FIELD_NAME_VALUE = "value";

    /**
     * Method returns the timestamp of the latest occurrence of the given service state value.
     *
     * @param enumStateValue the value to identify the state.
     * @param serviceState   the service state type which provides the occurrence list.
     *
     * @return the timestamp of the last occurrence.
     *
     * @throws NotAvailableException is thrown if the timestamp is not available.
     */
    public static Timestamp getLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, MessageOrBuilder serviceState) throws NotAvailableException {
        return getLatestValueOccurrence(enumStateValue.getValueDescriptor(), serviceState);
    }

    /**
     * Method returns the timestamp of the latest occurrence of the given service state value.
     *
     * @param enumValueDescriptor the enum value descriptor of the value to identify the state.
     * @param serviceState        the service state type which provides the occurrence list.
     *
     * @return the timestamp of the last occurrence.
     *
     * @throws NotAvailableException is thrown if the timestamp is not available.
     */
    public static Timestamp getLatestValueOccurrence(final EnumValueDescriptor enumValueDescriptor, MessageOrBuilder serviceState) throws NotAvailableException {
        FieldDescriptor mapFieldDescriptor = serviceState.getDescriptorForType().findFieldByName("last_value_occurrence");
        if (mapFieldDescriptor == null) {
            throw new NotAvailableException("Field[ast_value_occurrence] does not exist for type " + serviceState.getClass().getName());
        }
        return (Timestamp) ProtoBufFieldProcessor.getMapEntry(enumValueDescriptor, mapFieldDescriptor, serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumStateValue the state value which as been occurred.
     * @param timestamp      the timestamp of the occurrence.
     * @param serviceState   the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, long timestamp, Message.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumStateValue, Timestamp.newBuilder().setTime(timestamp).build(), serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumStateValue the state value which as been occurred.
     * @param timestamp      the timestamp of the occurrence.
     * @param serviceState   the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, final Timestamp timestamp, Message.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumStateValue.getValueDescriptor(), timestamp, serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumValueDescriptor the enum value descriptor of the state value which as been occurred.
     * @param timestamp           the timestamp of the occurrence.
     * @param serviceState        the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final EnumValueDescriptor enumValueDescriptor, long timestamp, Message.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumValueDescriptor, Timestamp.newBuilder().setTime(timestamp).build(), serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     * <p>
     * Note: This action takes only place if the given timestamp is newer than the currently stored one.
     *
     * @param enumValueDescriptor the enum value descriptor of the value which as been occurred.
     * @param timestamp           the timestamp of the occurrence.
     * @param serviceState        the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final EnumValueDescriptor enumValueDescriptor, final Timestamp timestamp, Message.Builder serviceState) throws CouldNotPerformException {

        // skip update if given timestamp is outdated.
        try {
            if (timestamp.getTime() <= getLatestValueOccurrence(enumValueDescriptor, serviceState).getTime()) {
                return;
            }
        } catch (NotAvailableException ex) {
            // no entry for state available, so continue with the update...
        }

        final Message entryMessage;

        try {
            final Class mapFieldEntryClass = Class.forName(serviceState.getClass().getDeclaringClass().getName() + INTERNAL_MAP_CLASS_NAME);
            final Message.Builder entryMessageBuilder = (Message.Builder) mapFieldEntryClass.getMethod("newBuilder").invoke(null);

            final FieldDescriptor keyDescriptor = entryMessageBuilder.build().getDescriptorForType().findFieldByName(FIELD_NAME_KEY);
            final FieldDescriptor valueDescriptor = entryMessageBuilder.build().getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);

            if (keyDescriptor == null) {
                throw new NotAvailableException("Field[KEY] does not exist for type " + entryMessageBuilder.getClass().getName());
            }

            if (valueDescriptor == null) {
                throw new NotAvailableException("Field[VALUE] does not exist for type " + entryMessageBuilder.getClass().getName());
            }

            entryMessageBuilder.setField(valueDescriptor, timestamp);
            entryMessageBuilder.setField(keyDescriptor, enumValueDescriptor);
            entryMessage = entryMessageBuilder.build();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state entry!", ex);
        }

        updateValueOccurrence(entryMessage, serviceState);
    }

    /**
     * This method updates the occurrence of the given service state value.
     * <p>
     * Note: The entry will just replaced and no timestamp validation is performed.
     *
     * @param entryMessage the entry containing the service state value and the timestamp.
     * @param serviceState the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateValueOccurrence(final Message entryMessage, final Message.Builder serviceState) throws CouldNotPerformException {
        final FieldDescriptor mapFieldDescriptor = serviceState.getDescriptorForType().findFieldByName(FIELD_NAME_LAST_VALUE_OCCURRENCE);
        if (mapFieldDescriptor == null) {
            throw new NotAvailableException("Field[last_value_occurrence] does not exist for type " + serviceState.getClass().getName());
        }
        ProtoBufFieldProcessor.putMapEntry(entryMessage, mapFieldDescriptor, serviceState);
    }

    /**
     * Returns the responsible action of the given service state.
     *
     * @param serviceState the service state that provides the responsible action
     *
     * @return the responsible action.
     * @throws NotAvailableException in case the state dose not provide any responsible action or if the message is not a valid service state.
     */
    public static ActionDescription getResponsibleAction(final Message serviceState) throws NotAvailableException {
        final FieldDescriptor responsibleActionFieldDescriptor;

        // resolve field
        try {
            responsibleActionFieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceState, "responsible_action");
        } catch (NotAvailableException ex) {
            new FatalImplementationErrorException("Given service state message does not provide the responsible action field!", serviceState, ex);
            throw new NotAvailableException("ActionDescription", ex);
        }

        // handle if responsible action is not set
        if (!serviceState.hasField(responsibleActionFieldDescriptor)) {
            throw new NotAvailableException("ActionDescription");
        }

        // resolve value
        return (ActionDescription) serviceState.getField(responsibleActionFieldDescriptor);
    }

    /**
     * Returns the responsible action of the given service state.
     *
     * @param serviceState the service state that provides the responsible action
     * @param alternative  a supplier that provides an alternative in case the action description is not available.
     *
     * @return the responsible action or its alternative.
     */
    public static ActionDescription getResponsibleAction(final Message serviceState, final Supplier<ActionDescription> alternative) {
        try {
            return getResponsibleAction(serviceState);
        } catch (NotAvailableException e) {
            return alternative.get();
        }
    }
}
