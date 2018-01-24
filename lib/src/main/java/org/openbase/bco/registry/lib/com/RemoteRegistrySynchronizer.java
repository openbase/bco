package org.openbase.bco.registry.lib.com;

/*-
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

import org.openbase.jul.pattern.MockUpFilter;
import org.openbase.jul.pattern.AbstractFilter;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;

import java.util.ArrayList;
import java.util.List;

import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.storage.registry.RemoteRegistry;
import org.slf4j.LoggerFactory;

/**
 * @param <M>
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 */
public class RemoteRegistrySynchronizer<M extends GeneratedMessage> implements Observer<M> {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoteRegistrySynchronizer.class);

    /**
     * The remote registry on which an update is notified.
     */
    private final RemoteRegistry<?, M, ?> remoteRegistry;

    /**
     * The field descriptor which is used for the internal registry synchronization.
     */
    private final FieldDescriptor[] fieldDescriptors;

    /**
     * A filter which can be used to filter out unwanted messages.
     */
    private final AbstractFilter<M> filter;

    /**
     * Create a registry synchronizer which does not filter.
     *
     * @param remoteRegistry   the registry on which the synchronized messages are notified
     * @param fieldDescriptors the fields which are used for the synchronization
     */
    public RemoteRegistrySynchronizer(final RemoteRegistry<?, M, ?> remoteRegistry, final FieldDescriptor[] fieldDescriptors) {
        this(remoteRegistry, fieldDescriptors, new MockUpFilter<>());
    }

    /**
     * Create a registry synchronizer which does filter.
     *
     * @param remoteRegistry   the registry on which the synchronized messages are notified
     * @param fieldDescriptors the fields which are used for the synchronization
     * @param filter           the filter according to which messages are filtered
     */
    public RemoteRegistrySynchronizer(final RemoteRegistry<?, M, ?> remoteRegistry, final FieldDescriptor[] fieldDescriptors, final AbstractFilter<M> filter) {
        this.remoteRegistry = remoteRegistry;
        this.fieldDescriptors = fieldDescriptors;
        this.filter = filter;
    }

    @Override
    public void update(final Observable<M> source, final M data) throws Exception {
        try {
            if (data == null) {
                throw new NotAvailableException("RegistryData");
            }

            final List<M> entryList = new ArrayList<>();
            for (final FieldDescriptor fieldDescriptor : fieldDescriptors) {
                for (int i = 0; i < data.getRepeatedFieldCount(fieldDescriptor); i++) {
                    entryList.add((M) data.getRepeatedField(fieldDescriptor, i));
                }
            }
            remoteRegistry.notifyRegistryUpdate(filter.filter(entryList));
        } catch (CouldNotPerformException | IndexOutOfBoundsException | ClassCastException | NullPointerException ex) {
            ExceptionPrinter.printHistory("Registry synchronization failed!", ex, LOGGER);
        }
    }
}
