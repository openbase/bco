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
import java.util.Map;
import org.openbase.bco.authentication.lib.SessionManager;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.com.RSBRemoteService;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.storage.registry.RemoteRegistry;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 * @param <KEY>
 * @param <M>
 * @param <MB>
 */
public class SynchronizedRemoteRegistry<KEY, M extends GeneratedMessage, MB extends M.Builder<MB>> extends RemoteRegistry<KEY, M, MB> implements Activatable {
    
    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SynchronizedRemoteRegistry.class);

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
    private final RemoteRegistrySynchronizer<M> remoteRegistrySynchronizer;
    
    private boolean active;
    
    private AbstractFilter filter;

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
            this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer(this, fieldDescriptors);
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    /**
     *
     * @param remoteService The remote service to get informed about data updates.
     * @param filter the filter which is used to synchronize the messages
     * @param protobufFieldNumbers The field numbers to identify the descriptor fields which are used for the internal registry synchronization.
     * @throws InstantiationException is thrown in case the instantiation fails.
     */
    public SynchronizedRemoteRegistry(final RSBRemoteService remoteService, final AbstractFilter<M> filter, final int... protobufFieldNumbers) throws InstantiationException {
        try {
            this.fieldDescriptors = ProtoBufFieldProcessor.getFieldDescriptors(remoteService.getDataClass(), protobufFieldNumbers);
            this.remoteService = remoteService;
            this.filter = filter;
            this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer(this, fieldDescriptors, filter);
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
        this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer(this, fieldDescriptors);
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
        this.remoteRegistrySynchronizer = new RemoteRegistrySynchronizer(this, fieldDescriptors);
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
        if (active) {
            logger.warn("Already activated");
            return;
        }
        remoteService.addDataObserver(remoteRegistrySynchronizer);

        // trigger initial synch if data is available
        if (remoteService.isDataAvailable()) {
            try {
                remoteRegistrySynchronizer.update(null, remoteService.getData());
            } catch (Exception ex) {
                ExceptionPrinter.printHistory("Initial synchronization of " + this + " failed!", ex, LOGGER);
            }
        }
        //TODO: this is a little bit hacked
        // if filter by authorization also update when something in the login changes
        if (filter != null && filter instanceof AuthorizationFilter) {
            SessionManager.getInstance().getLoginObervable().addObserver((Observable<Boolean> source1, Boolean data) -> {
                if (remoteService.isDataAvailable()) {
                    remoteRegistrySynchronizer.update(null, remoteService.getData());
                }
            });
        }
        
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
            return getClass().getSimpleName() + "[" + (remoteService != null ? remoteService.toString() : "?") + "]";
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
    
    public AbstractFilter getFilter() {
        return filter;
    }
}
