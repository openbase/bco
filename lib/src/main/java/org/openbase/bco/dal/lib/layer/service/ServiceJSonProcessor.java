package org.openbase.bco.dal.lib.layer.service;

/*
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
import com.google.protobuf.Message;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class ServiceJSonProcessor extends ProtoBufJSonProcessor {

    public ServiceJSonProcessor() {
        super();
    }

    /**
     * Serialize a serviceAttribute which is a protoBuf message.
     * This method removes timestamp and responsible action fields.
     *
     * @param serviceAttribute the service attribute which is serialized
     * @return a jSon string representation of the service attribute
     * @throws CouldNotPerformException if the serialization fails or the service attribute does not contain any context
     *                                  <p>
     *                                  TODO: release: change parameter type to message since java primitives cannot be de-/serialized anymore anyway
     */
    @Override
    public String serialize(final Object serviceAttribute) throws CouldNotPerformException {
        final List<String> filteredFieldList = new ArrayList<>();
        filteredFieldList.add(TimestampProcessor.TIMESTEMP_FIELD.toLowerCase());
        filteredFieldList.add(Service.RESPONSIBLE_ACTION_FIELD_NAME);
        return serialize((Message) serviceAttribute, filteredFieldList);
    }

    /**
     * Serialize a serviceAttribute and filter a list of fields beforehand.
     *
     * @param serviceAttribute  the service attribute which is serialized
     * @param filteredFieldList a list of field names which shall be filtered before serialization
     * @return a jSon string representation of the service attribute without the filtered fields
     * @throws CouldNotPerformException if the serialization fails or the service attribute does not contain any context
     */
    public String serialize(final Message serviceAttribute, final List<String> filteredFieldList) throws CouldNotPerformException {
        final Message.Builder builder = serviceAttribute.toBuilder();

        for (String filteredField : filteredFieldList) {
            FieldDescriptor fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(builder, filteredField);
            if (!builder.hasField(fieldDescriptor)) {
                continue;
            }

            builder.clearField(fieldDescriptor);
        }

        return super.serialize(builder.build());
    }
}
