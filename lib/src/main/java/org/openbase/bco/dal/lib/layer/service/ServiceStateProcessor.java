package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import rst.timing.TimestampType.Timestamp;

import java.lang.reflect.InvocationTargetException;

public class ServiceStateProcessor {

    public static final String INTERNAL_MAP_CLASS_NAME = "$MapFieldEntry";
    public static final String FIELD_NAME_LAST_FALUE_OCCURRENCE = "last_value_occurrence";
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
     * @throws CouldNotPerformException is thrown if the timestamp is not available.
     */
    public static Timestamp getLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, GeneratedMessage.Builder serviceState) throws CouldNotPerformException {
        FieldDescriptor mapFieldDescriptor = serviceState.getDescriptorForType().findFieldByName("last_value_occurrence");
        if (mapFieldDescriptor == null) {
            throw new NotAvailableException("Field[ast_value_occurrence] does not exist for type " + serviceState.getClass().getName());
        }
        return (Timestamp) ProtoBufFieldProcessor.getMapEntry(enumStateValue, mapFieldDescriptor, serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     *
     * @param enumStateValue the state value which as been occurred.
     * @param timestamp      the timestamp of the occurrence.
     * @param serviceState   the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, long timestamp, GeneratedMessage.Builder serviceState) throws CouldNotPerformException {
        updateLatestValueOccurrence(enumStateValue, Timestamp.newBuilder().setTime(timestamp).build(), serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     *
     * @param enumStateValue the state value which as been occurred.
     * @param timestamp      the timestamp of the occurrence.
     * @param serviceState   the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final ProtocolMessageEnum enumStateValue, final Timestamp timestamp, GeneratedMessage.Builder serviceState) throws CouldNotPerformException {
        final Message entryMessage;

        try {
            final Class mapFieldEntryClass = Class.forName(serviceState.getClass().getDeclaringClass().getName() + INTERNAL_MAP_CLASS_NAME);
            final GeneratedMessage.Builder entryMessageBuilder = (GeneratedMessage.Builder) mapFieldEntryClass.getMethod("newBuilder").invoke(null);

            final FieldDescriptor key = entryMessageBuilder.build().getDescriptorForType().findFieldByName(FIELD_NAME_KEY);
            final FieldDescriptor value = entryMessageBuilder.build().getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);

            entryMessageBuilder.setField(value, timestamp);
            entryMessageBuilder.setField(key, enumStateValue.getValueDescriptor());
            entryMessage = entryMessageBuilder.build();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException ex) {
            throw new CouldNotPerformException("Could not build service state entry!", ex);
        }

        updateLatestValueOccurrence(entryMessage, serviceState);
    }

    /**
     * This method updates the last occurrence of the given service state value.
     *
     * @param entryMessage the entry containing the service state value and the timestamp.
     * @param serviceState the service state message which holds the service value.
     *
     * @throws CouldNotPerformException is thrown if the update could not be performed.
     */
    public static void updateLatestValueOccurrence(final Message entryMessage, GeneratedMessage.Builder serviceState) throws CouldNotPerformException {
        FieldDescriptor mapFieldDescriptor = serviceState.getDescriptorForType().findFieldByName(FIELD_NAME_LAST_FALUE_OCCURRENCE);
        if (mapFieldDescriptor == null) {
            throw new NotAvailableException("Field[last_value_occurrence] does not exist for type " + serviceState.getClass().getName());
        }
        ProtoBufFieldProcessor.putMapEntry(entryMessage, mapFieldDescriptor, serviceState);
    }
}
