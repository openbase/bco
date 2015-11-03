/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.registry;

import com.google.protobuf.GeneratedMessage;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPTestMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.MultiException;
import de.citec.jul.exception.printer.LogLevel;
import de.citec.jul.extension.protobuf.IdentifiableMessage;
import de.citec.jul.extension.protobuf.ProtobufListDiff;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.pattern.Factory;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.jul.schedule.RecurrenceEventFilter;
import de.citec.jul.storage.registry.Registry;
import de.citec.jul.storage.registry.RemoteRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mpohling
 * @param <KEY>
 * @param <ENTRY>
 * @param <CONFIG_M>
 * @param <CONFIG_MB>
 */
public abstract class RegistrySynchronizer<KEY, ENTRY extends Identifiable<KEY>, CONFIG_M extends GeneratedMessage, CONFIG_MB extends CONFIG_M.Builder<CONFIG_MB>> {

    private static final Logger logger = LoggerFactory.getLogger(RegistrySynchronizer.class);
    private final Registry<KEY, ENTRY> registry;
    private final RemoteRegistry<KEY, CONFIG_M, CONFIG_MB, ?> remoteRegistry;
    private final Observer<Map<KEY, IdentifiableMessage<KEY, CONFIG_M, CONFIG_MB>>> remoteChangeObserver;
    private final RecurrenceEventFilter recurrenceSyncFilter;
    private final ProtobufListDiff<KEY, CONFIG_M, CONFIG_MB> entryConfigDiff;
    private final Factory<ENTRY, CONFIG_M> factory;

    public RegistrySynchronizer(final Registry<KEY, ENTRY> registry, final RemoteRegistry<KEY, CONFIG_M, CONFIG_MB, ?> remoteRegistry, final Factory<ENTRY, CONFIG_M> factory) throws de.citec.jul.exception.InstantiationException {
        try {
            this.registry = registry;
            this.remoteRegistry = remoteRegistry;
            this.entryConfigDiff = new ProtobufListDiff<>();
            this.factory = factory;
            this.recurrenceSyncFilter = new RecurrenceEventFilter(15000) {

                @Override
                public void relay() throws Exception {
                    internalSync();
                }
            };

            this.remoteChangeObserver = (Observable<Map<KEY, IdentifiableMessage<KEY, CONFIG_M, CONFIG_MB>>> source, Map<KEY, IdentifiableMessage<KEY, CONFIG_M, CONFIG_MB>> data) -> {
                sync();
            };

        } catch (Exception ex) {
            throw new de.citec.jul.exception.InstantiationException(this, ex);
        }
    }

    public void init() throws CouldNotPerformException, InterruptedException {
        this.remoteRegistry.addObserver(remoteChangeObserver);
        try {
            this.internalSync();
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Initial sync failed!", ex), logger, LogLevel.ERROR);
            if (JPService.getProperty(JPTestMode.class).getValue()) {
                throw ex;
            }
        }
    }

    public void shutdown() {
        this.remoteRegistry.removeObserver(remoteChangeObserver);
        this.recurrenceSyncFilter.cancel();
    }

    private void sync() {
        recurrenceSyncFilter.trigger();
    }

    private synchronized void internalSync() throws CouldNotPerformException, InterruptedException {
        logger.info("Perform registry sync...");

        try {
            entryConfigDiff.diff(remoteRegistry.getMessages());

            MultiException.ExceptionStack removeExceptionStack = null;
            for (CONFIG_M config : entryConfigDiff.getRemovedMessageMap().getMessages()) {
                try {
                    remove(config);
                } catch (Exception ex) {
                    removeExceptionStack = MultiException.push(this, ex, removeExceptionStack);
                }
            }

            MultiException.ExceptionStack updateExceptionStack = null;
            for (CONFIG_M config : entryConfigDiff.getUpdatedMessageMap().getMessages()) {
                try {
                    if (verifyConfig(config)) {
                        update(config);
                    } else {
                        remove(config);
                    }
                } catch (Exception ex) {
                    updateExceptionStack = MultiException.push(this, ex, updateExceptionStack);
                }
            }

            MultiException.ExceptionStack registerExceptionStack = null;
            for (CONFIG_M config : entryConfigDiff.getNewMessageMap().getMessages()) {
                try {
                    if (verifyConfig(config)) {
                        register(config);
                    }
                } catch (Exception ex) {
                    registerExceptionStack = MultiException.push(this, ex, registerExceptionStack);
                }
            }

            int errorCounter = MultiException.size(removeExceptionStack) + MultiException.size(updateExceptionStack) + MultiException.size(registerExceptionStack);
            logger.info(entryConfigDiff.getChangeCounter() + " registry changes applied. " + errorCounter + " are skipped.");

            // build exception cause chain.
            MultiException.ExceptionStack exceptionStack = null;
            try {
                MultiException.checkAndThrow("Could not remove all entries!", removeExceptionStack);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
            try {
                MultiException.checkAndThrow("Could not update all entries!", updateExceptionStack);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
            try {
                MultiException.checkAndThrow("Could not register all entries!", registerExceptionStack);
            } catch (CouldNotPerformException ex) {
                exceptionStack = MultiException.push(this, ex, exceptionStack);
            }
            MultiException.checkAndThrow("Could not sync all entries!", exceptionStack);

        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Entry registry sync failed!", ex);
        }
    }

    public ENTRY register(final CONFIG_M config) throws CouldNotPerformException, InterruptedException {
        return registry.register(factory.newInstance(config));
    }

    public ENTRY update(final CONFIG_M config) throws CouldNotPerformException, InterruptedException {
        return registry.update(factory.newInstance(config));
    }

    public ENTRY remove(final CONFIG_M config) throws CouldNotPerformException, InterruptedException {
        return registry.remove(remoteRegistry.getKey(config));
    }

    /**
     * Method should return true if the given configurations is valid, otherwise false. 
     * This default implementation accepts all configurations. To implement a custom verification just overwrite this method.
     *
     * @param config
     * @return
     */
    public boolean verifyConfig(final CONFIG_M config) {
        return true;
    }
}
