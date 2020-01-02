package org.openbase.bco.dal.lib.layer.service;

/*
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

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.TimestampProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ServiceJSonProcessor extends ProtoBufJSonProcessor {

    public static final List<String> DEFAULT_FILTER_LIST;

    static {
        DEFAULT_FILTER_LIST = new ArrayList<>();
        DEFAULT_FILTER_LIST.add(TimestampProcessor.TIMESTAMP_FIELD_NAME);
        DEFAULT_FILTER_LIST.add(Service.RESPONSIBLE_ACTION_FIELD_NAME);
    }

    public ServiceJSonProcessor() {
        super();
    }

    /**
     * Serialize a serviceState which is a protoBuf message.
     * This method removes timestamp and responsible action fields.
     *
     * @param serviceState the service attribute which is serialized
     *
     * @return a jSon string representation of the service attribute
     *
     * @throws CouldNotPerformException if the serialization fails or the service attribute does not contain any context
     */
    @Override
    public String serialize(final Message serviceState) throws CouldNotPerformException {
        return serialize(serviceState, DEFAULT_FILTER_LIST);
    }

    /**
     * Serialize a serviceState which is a protoBuf message.
     * This method removes timestamp and responsible action fields.
     *
     * @param serviceStateBuilder the service attribute which is serialized
     *
     * @return a jSon string representation of the service attribute
     *
     * @throws CouldNotPerformException if the serialization fails or the service attribute does not contain any context
     */
    @Override
    public String serialize(final Message.Builder serviceStateBuilder) throws CouldNotPerformException {
        return serialize(serviceStateBuilder.build(), DEFAULT_FILTER_LIST);
    }

    /**
     * Serialize a serviceState and filter a list of fields beforehand.
     *
     * @param serviceState      the service attribute which is serialized
     * @param filteredFieldList a list of field names which shall be filtered before serialization
     *
     * @return a jSon string representation of the service attribute without the filtered fields
     *
     * @throws CouldNotPerformException if the serialization fails or the service attribute does not contain any context
     */
    public String serialize(final Message serviceState, final List<String> filteredFieldList) throws CouldNotPerformException {
        return serialize(serviceState.toBuilder(), filteredFieldList);
    }

    /**
     * Serialize a serviceState and filter a list of fields beforehand.
     *
     * @param serviceStateBuilder the service attribute which is serialized
     * @param filteredFieldList   a list of field names which shall be filtered before serialization
     *
     * @return a jSon string representation of the service attribute without the filtered fields
     *
     * @throws CouldNotPerformException if the serialization fails or the service attribute does not contain any context
     */
    public String serialize(final Message.Builder serviceStateBuilder, final List<String> filteredFieldList) throws CouldNotPerformException {

        for (String filteredField : filteredFieldList) {
            FieldDescriptor fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(serviceStateBuilder, filteredField);
            if (!serviceStateBuilder.hasField(fieldDescriptor)) {
                continue;
            }

            serviceStateBuilder.clearField(fieldDescriptor);
        }

        return super.serialize(serviceStateBuilder);
    }
}
