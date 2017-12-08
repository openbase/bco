package org.openbase.bco.dal.lib.layer.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import com.google.protobuf.Message.Builder;
import com.google.protobuf.Message;
import org.openbase.jul.extension.protobuf.MessageObservable;
import org.openbase.jul.pattern.provider.DataProvider;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 * @param <M>
 */
public class ServiceDataFilteredObservable<M extends Message> extends MessageObservable<M> {

    public static final String RESPONSIBLE_ACTION_FIELD = "responsible_action";

    public ServiceDataFilteredObservable(final DataProvider<M> source) {
        super(source);

        this.setHashGenerator((M value) -> removeResponsibleActoin(removeTimestamps(value.toBuilder())).build().hashCode());
    }

    private Builder removeResponsibleActoin(final Builder builder) {
        Descriptors.Descriptor descriptorForType = builder.getDescriptorForType();
        descriptorForType.getFields().stream().filter((field) -> (field.getType() == Descriptors.FieldDescriptor.Type.MESSAGE && field.getName().equals(RESPONSIBLE_ACTION_FIELD))).forEachOrdered((field) -> {
            builder.clearField(field);
        });
        return builder;
    }

}
