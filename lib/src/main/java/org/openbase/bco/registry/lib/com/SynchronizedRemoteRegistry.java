package org.openbase.bco.registry.lib.com;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.storage.registry.RemoteRegistry;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <KEY>
 * @param <M>
 * @param <MB>
 */
public class SynchronizedRemoteRegistry<KEY, M extends GeneratedMessage, MB extends M.Builder<MB>> extends RemoteRegistry<KEY, M, MB> implements Activatable {

    /**
     * The field descriptor which is used for the internal registry synchronization.
     */
    private final Descriptors.FieldDescriptor fieldDescriptor;

    /**
     * The remote service to get informed about data updates.
     */
    private final RSBRemoteService<M> remoteService;

    /**
     * The internal data synchronizer which synchronizes the remote registry via the remote service.
     */
    private final RemoteRegistrySynchronizer remoteRegistrySynchronizer;

    private boolean active;

    /**
     *
     * @param protobufFieldNumber The field number to identify the descriptor field which is used for the internal registry synchronization.
     * @param remoteService The remote service to get informed about data updates.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(final int protobufFieldNumber, final RSBRemoteService remoteService) throws InstantiationException {
        try {
            this.fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptor(protobufFieldNumber, remoteService.getDataClass());
            this.remoteService = remoteService;
            this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     *
     * @param fieldDescriptor The field descriptor which is used for the internal registry synchronization.
     * @param remoteService The remote service to get informed about data updates.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(final Descriptors.FieldDescriptor fieldDescriptor, final RSBRemoteService<M> remoteService) throws InstantiationException {
        this.fieldDescriptor = fieldDescriptor;
        this.remoteService = remoteService;
        this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer();
    }

    /**
     *
     * @param fieldDescriptor The field descriptor which is used for the internal registry synchronization.
     * @param remoteService The remote service to get informed about data updates.
     * @param internalMap the internal map instance of this registry.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(Descriptors.FieldDescriptor fieldDescriptor, RSBRemoteService<M> remoteService, Map<KEY, IdentifiableMessage<KEY, M, MB>> internalMap) throws InstantiationException {
        super(internalMap);
        this.fieldDescriptor = fieldDescriptor;
        this.remoteService = remoteService;
        this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer();
    }

    public Descriptors.FieldDescriptor getFieldDescriptor() {
        return fieldDescriptor;
    }

    public RSBRemoteService<M> getRemoteService() {
        return remoteService;
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        remoteService.addDataObserver(remoteRegistrySynchronizer);
        active = true;
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        remoteService.removeDataObserver(remoteRegistrySynchronizer);
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    class RemoteRegistrySynchronizer implements Observer<M> {

        @Override
        public void update(Observable<M> source, M data) throws Exception {
            try {
                int entryCount = data.getRepeatedFieldCount(fieldDescriptor);
                List<M> entryList = new ArrayList<>();
                for (int i = 0; i < entryCount; i++) {
                    entryList.add((M) data.getRepeatedField(fieldDescriptor, i));
                }
                notifyRegistryUpdate(entryList);
            } catch (CouldNotPerformException | IndexOutOfBoundsException | ClassCastException | NullPointerException ex) {
                ExceptionPrinter.printHistory("Registry synchronization failed!", ex, logger);
            }
        }
    }
}
