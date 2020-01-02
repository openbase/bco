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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.Type;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import org.openbase.jul.extension.protobuf.MessageObservable;

/**
 * Observable for service data. It uses a custom hash generator that filters responsible actions, timestamps
 * and last value occurrences from the service data type so that changes in these values do not trigger a notification.
 *
 * @param <M> the service data type handled by this observable
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class ServiceDataFilteredObservable<M extends Message> extends MessageObservable<ServiceStateProvider<M>, M> {


    /**
     * Construct a service data filtered observable.
     *
     * @param source {@inheritDoc}
     */
    public ServiceDataFilteredObservable(final ServiceStateProvider<M> source) {
        super(source);
        this.setHashGenerator((M value) -> clearUnobservedField(value.toBuilder()).build().hashCode());
    }

    /**
     * Clear responsible action, timestamp and last value occurrence fields from a builder if they are available.
     *
     * @param builder the builder from which the fields will be cleared.
     *
     * @return the builder with cleared fields.
     */
    private Builder clearUnobservedField(final Builder builder) {
        final Descriptors.Descriptor descriptorForType = builder.getDescriptorForType();
        for (final FieldDescriptor field : descriptorForType.getFields()) {
            if (field.getType() == Type.MESSAGE && field.getName().equals(Service.RESPONSIBLE_ACTION_FIELD_NAME)) {
                builder.clearField(field);
            }

            if (field.isRepeated() && field.getName().equals(ServiceStateProcessor.FIELD_NAME_LAST_VALUE_OCCURRENCE)) {
                builder.clearField(field);
            }
        }
        return removeTimestamps(builder);
    }
}
