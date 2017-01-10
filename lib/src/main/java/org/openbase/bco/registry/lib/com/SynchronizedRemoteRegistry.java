package org.openbase.bco.registry.lib.com;

/*
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
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
    private final FieldDescriptor[] fieldDescriptors;

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
     * @param remoteService The remote service to get informed about data updates.
     * @param protobufFieldNumbers The field numbers to identify the descriptor fields which are used for the internal registry synchronization.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(final RSBRemoteService remoteService, final int... protobufFieldNumbers) throws InstantiationException {
        try {
            this.fieldDescriptors = ProtoBufFieldProcessor.getFieldDescriptors(remoteService.getDataClass(), protobufFieldNumbers);
            this.remoteService = remoteService;
            this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     *
     * @param remoteService The remote service to get informed about data updates.
     * @param fieldDescriptors The field descriptors which are used for the internal registry synchronization.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(final RSBRemoteService<M> remoteService, final Descriptors.FieldDescriptor... fieldDescriptors) throws InstantiationException {
        this.fieldDescriptors = fieldDescriptors;
        this.remoteService = remoteService;
        this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer();
    }

    /**
     *
     * @param remoteService The remote service to get informed about data updates.
     * @param internalMap the internal map instance of this registry.
     * @param fieldDescriptors The field descriptors which are used for the internal registry synchronization.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(final RSBRemoteService<M> remoteService, final Map<KEY, IdentifiableMessage<KEY, M, MB>> internalMap, final Descriptors.FieldDescriptor... fieldDescriptors) throws InstantiationException {
        super(internalMap);
        this.fieldDescriptors = fieldDescriptors;
        this.remoteService = remoteService;
        this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer();
    }

    /**
     * Method returns the list of field descriptors which are used to identify the protobuf field that are merged into the registry.
     *
     * @return
     */
    public FieldDescriptor[] getFieldDescriptors() {
        return fieldDescriptors;
    }

    /**
     * Method returns the internal remote service which is used to detect external data changes.
     *
     * @return
     */
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

    @Override
    public String getName() {
        if (fieldDescriptors == null || fieldDescriptors.length == 0) {
            return getClass().getSimpleName() + "[" + (remoteService != null ? remoteService.toString() : "?")  + "]";
        } else {
            String fieldDescritorNames = "[";
            fieldDescritorNames += fieldDescriptors[0].getName();
            for (int i = 1; i < fieldDescriptors.length; ++i) {
                fieldDescritorNames += ", " + fieldDescriptors[i].getName();
            }
            fieldDescritorNames += "]";

            return getClass().getSimpleName() + "[" + remoteService.toString() + "]" + fieldDescritorNames;
        }
    }

    class RemoteRegistrySynchronizer implements Observer<M> {

        @Override
        public void update(Observable<M> source, M data) throws Exception {
            try {
                if (data == null) {
                    throw new NotAvailableException("RegistryData");
                }
                int entryCount;
                List<M> entryList = new ArrayList<>();
                for (final FieldDescriptor fieldDescriptor : fieldDescriptors) {
                    entryCount = data.getRepeatedFieldCount(fieldDescriptor);
                    for (int i = 0; i < entryCount; i++) {
                        entryList.add((M) data.getRepeatedField(fieldDescriptor, i));
                    }
                }
                notifyRegistryUpdate(entryList);
            } catch (CouldNotPerformException | IndexOutOfBoundsException | ClassCastException | NullPointerException ex) {
                ExceptionPrinter.printHistory("Registry synchronization failed!", ex, logger);
            }
        }
    }
}
