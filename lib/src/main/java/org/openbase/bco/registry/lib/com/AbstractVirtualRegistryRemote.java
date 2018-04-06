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

import com.google.protobuf.Descriptors;
import com.google.protobuf.GeneratedMessage;
import org.openbase.jps.core.JPService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.protobuf.processing.ProtoBufFieldProcessor;
import org.openbase.jul.extension.rsb.scope.jp.JPScope;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.RegistryRemote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @param <M>
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractVirtualRegistryRemote<M extends GeneratedMessage> extends AbstractRegistryRemote<M> {

    private final Map<SynchronizedRemoteRegistry, Descriptors.FieldDescriptor> remoteRegistryFieldDescriptorMap;
    private final Map<SynchronizedRemoteRegistry, RegistryRemote<? extends GeneratedMessage>> remoteRegistrySyncMap;
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

    protected void bindRegistryRemoteToRemoteRegistry(SynchronizedRemoteRegistry remoteRegistry, RegistryRemote<? extends GeneratedMessage> registryRemote, Integer fieldNumber) throws CouldNotPerformException {
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

    private boolean virtualRegistryInitiallySynchronized = false;

    @Override
    public boolean isDataAvailable() {
        for (RegistryRemote registryRemote : registryRemotes) {
            if (!registryRemote.isDataAvailable()) {
                return false;
            }
        }

        // TODO release:
        // workaround, should be removed in release scrab
        virtualRegistryInitiallySynchronized = virtualRegistryInitiallySynchronized || isVirtualRegistrySynchronized();

        return super.isDataAvailable() && virtualRegistryInitiallySynchronized;
    }

    private void waitForVirtualRegistrySync() throws InterruptedException {
        synchronized (virtualRegistrySyncLock) {
            while (!isVirtualRegistrySynchronized()) {
                virtualRegistrySyncLock.wait();
            }
        }
    }

    public boolean isVirtualRegistrySynchronized() {
        // if not synchronized it could happen that equal message count is called from two different threads
        // which can modify the computed integers before checking
        synchronized (virtualRegistrySyncLock) {
            for (SynchronizedRemoteRegistry remoteRegistry : remoteRegistrySyncMap.keySet()) {
                try {
                    final List registryRemoteMessageList = new ArrayList((List) remoteRegistrySyncMap.get(remoteRegistry).getData().getField(remoteRegistryFieldDescriptorMap.get(remoteRegistry)));
                    final List registryRemoteFilteredMessageList = remoteRegistry.getFilter().filter(registryRemoteMessageList);

                    // init counts
                    int registryRemoteMessageCount = registryRemoteMessageList.size();
                    int registryRemoteFilteredMessageCount = registryRemoteFilteredMessageList.size();

                    // just print filtered messages in test mode
                    if (JPService.testMode()) {
                        if (registryRemoteMessageCount != registryRemoteFilteredMessageCount) {
                            logger.info(this + " has a been filtered for Field[" + remoteRegistryFieldDescriptorMap.get(remoteRegistry).getName() + "] from " + registryRemoteMessageCount + " to " + registryRemoteFilteredMessageCount);
                            for (final Object message : registryRemoteMessageList) {
                                if (!registryRemoteFilteredMessageList.contains(message)) {
                                    logger.info("Filtered Message[" + new IdentifiableMessage((GeneratedMessage) message).generateMessageDescription() + "] because permission was denied.");
                                }
                            }
                        }
                    }

                    // check if the remote registry was fully synchronized with this registry remote.
                    int remoteRegistryMessageCount = remoteRegistry.getMessages().size();
                    if (registryRemoteFilteredMessageCount != remoteRegistryMessageCount) {
                        if (JPService.testMode()) {
                            logger.info("MessageCount for [" + remoteRegistry + "] is not correct. Expected " + registryRemoteFilteredMessageCount + " but is " + remoteRegistryMessageCount);
                            for (Object message : registryRemoteFilteredMessageList) {
                                if (!remoteRegistry.getMessages().contains(message)) {
                                    logger.info("Message[" + new IdentifiableMessage((GeneratedMessage) message).generateMessageDescription() + "] was not synchronized from the registry remote into the internal remote registry!");
                                }
                            }
                            for (Object message : remoteRegistry.getMessages()) {
                                if (!registryRemoteFilteredMessageList.contains(message)) {
                                    logger.info("Message[" + new IdentifiableMessage((GeneratedMessage) message).generateMessageDescription() + "] was not removed form the internal remote registry!");
                                }
                            }
                        }
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
}
