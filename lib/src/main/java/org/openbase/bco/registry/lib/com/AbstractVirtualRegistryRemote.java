package org.openbase.bco.registry.lib.com;

/*-
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
import com.google.protobuf.GeneratedMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.RegistryRemote;
import org.openbase.jul.storage.registry.RemoteRegistry;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 * @param <M>
 */
public abstract class AbstractVirtualRegistryRemote<M extends GeneratedMessage> extends AbstractRegistryRemote<M> {

    private final Map<RemoteRegistry, Descriptors.FieldDescriptor> remoteRegistryFieldDescriptorMap;
    private final Map<RemoteRegistry, RegistryRemote<? extends GeneratedMessage>> remoteRegistrySyncMap;
    private final List<RegistryRemote<? extends GeneratedMessage>> registryRemotes;

    private final SyncObject virtualRegistrySyncLock = new SyncObject("RegistryRemoteVirtualSyncLock");
    private final Observer synchronisationObserver;

    public AbstractVirtualRegistryRemote(Class<? extends JPScope> jpScopePropery, Class<M> dataClass) {
        super(jpScopePropery, dataClass);

        this.remoteRegistryFieldDescriptorMap = new HashMap<>();
        this.remoteRegistrySyncMap = new HashMap<>();
        this.registryRemotes = new ArrayList<>();
        this.synchronisationObserver = (Observer) (Observable source, Object data1) -> {
            synchronized (virtualRegistrySyncLock) {
                virtualRegistrySyncLock.notifyAll();
            }
        };
    }

    @Override
    protected void postInit() throws InitializationException, InterruptedException {
        super.postInit();
        registryRemotes.clear();
        registerRegistryRemotes();
        bindRegistryRemoteToRemoteRegistries();
    }

    @Override
    public void activate() throws InterruptedException, CouldNotPerformException {
        super.activate();
        getRemoteRegistries().forEach((remoteRegistry) -> {
            remoteRegistry.addObserver(synchronisationObserver);
        });
        
        // initial check
        synchronized (virtualRegistrySyncLock) {
            virtualRegistrySyncLock.notifyAll();
        }
    }

    @Override
    public void deactivate() throws InterruptedException, CouldNotPerformException {
        getRemoteRegistries().forEach((remoteRegistry) -> {
            remoteRegistry.removeObserver(synchronisationObserver);
        });
        super.deactivate();
    }

    protected void registerRegistryRemote(RegistryRemote<? extends GeneratedMessage> registryRemote) {
        this.registryRemotes.add(registryRemote);
    }

    protected abstract void registerRegistryRemotes() throws InitializationException, InterruptedException;

    protected void bindRegistryRemoteToRemoteRegistry(RemoteRegistry remoteRegistry, RegistryRemote<? extends GeneratedMessage> registryRemote, Integer fieldNumber) throws CouldNotPerformException {
        try {
            Descriptors.FieldDescriptor fieldDescriptor = null;
            try {
                fieldDescriptor = ProtoBufFieldProcessor.getFieldDescriptors(registryRemote.getDataClass(), fieldNumber)[0];
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Invalid field descriptor for [" + registryRemote.getDataClass().getSimpleName() + "]", ex);
            }

            if (!registryRemotes.contains(registryRemote)) {
                throw new CouldNotPerformException("Trying to bind to unregistered registryRemote");
            }

            remoteRegistryFieldDescriptorMap.put(remoteRegistry, fieldDescriptor);
            remoteRegistrySyncMap.put(remoteRegistry, registryRemote);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not bind fieldNumber[" + fieldNumber + "] of [" + registryRemote.getDataClass().getSimpleName() + "] to remoteRegistry[" + remoteRegistry + "]", ex);
        }
    }

    protected abstract void bindRegistryRemoteToRemoteRegistries();

    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        for (RegistryRemote registryRemote : registryRemotes) {
            registryRemote.waitForData();
        }
        super.waitForData();
        waitForVirtualRegistrySync();
    }

    @Override
    public Boolean isReady() throws InterruptedException {
        return isDataAvailable() && super.isReady();
    }

    @Override
    public boolean isDataAvailable() {
        for (RegistryRemote registryRemote : registryRemotes) {
            if (!registryRemote.isDataAvailable()) {
                return false;
            }
        }

        return super.isDataAvailable() && equalMessageCounts();
    }

    private void waitForVirtualRegistrySync() throws CouldNotPerformException, InterruptedException {
        synchronized (virtualRegistrySyncLock) {
            while (!equalMessageCounts()) {
                virtualRegistrySyncLock.wait();
            }
        }
    }

    private boolean equalMessageCounts() {
        for (RemoteRegistry remoteRegistry : remoteRegistrySyncMap.keySet()) {
            try {
                if (remoteRegistrySyncMap.get(remoteRegistry).getData().getRepeatedFieldCount(remoteRegistryFieldDescriptorMap.get(remoteRegistry)) != remoteRegistry.getMessages().size()) {
                    return false;
                }
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not check if registries contains the same amount of entries!", ex, logger);
                return false;
            }
        }
        return true;
    }
}
