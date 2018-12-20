package org.openbase.bco.registry.lib.generator;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import com.google.protobuf.Message;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.IdGenerator;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.type.language.LabelType;

import java.util.UUID;

import static org.openbase.jul.iface.provider.LabelProvider.TYPE_FIELD_LABEL;

/**
 * @param <M>
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public class UUIDGenerator<M extends Message> implements IdGenerator<String, M> {

    @Override
    public String generateId(final M message) {
        // in case we are performing a unit test let's generate a human readable uuid.
        try {
            if (JPService.testMode()) {
                if (message.getDescriptorForType().findFieldByName(TYPE_FIELD_LABEL) != null) {
                    if (message.hasField(message.getDescriptorForType().findFieldByName(TYPE_FIELD_LABEL))) {
                        LabelType.Label label = (LabelType.Label) message.getField(message.getDescriptorForType().findFieldByName(TYPE_FIELD_LABEL));
                        return LabelProcessor.getBestMatch(label) + ":" + UUID.randomUUID().toString();
                    }
                }
            }
        } catch (NotAvailableException ex) {
            // label not available so just use normal uuid
        }
        return UUID.randomUUID().toString();
    }
}
