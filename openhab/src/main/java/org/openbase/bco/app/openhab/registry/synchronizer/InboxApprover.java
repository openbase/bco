package org.openbase.bco.app.openhab.registry.synchronizer;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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


import org.eclipse.smarthome.config.discovery.dto.DiscoveryResultDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.iface.Activatable;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class InboxApprover implements Activatable {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboxApprover.class);

    private final Observer<DataProvider<DiscoveryResultDTO>, DiscoveryResultDTO> observer;
    private final InboxAddedObservable inboxAddedObservable;
    private final InboxUpdatedObservable inboxUpdatedObservable;
    private boolean active;

    public InboxApprover() {
        this.active = false;
        this.inboxAddedObservable = new InboxAddedObservable();
        this.inboxUpdatedObservable = new InboxUpdatedObservable();
        observer = (source, discoveryResultDTO) -> {
            try {
                // approve everything from the bco binding
                if (discoveryResultDTO.thingUID.startsWith("bco")) {
                    OpenHABRestCommunicator.getInstance().approve(discoveryResultDTO.thingUID, discoveryResultDTO.label);
                    return;
                }
                // try to find a device class matching the discovery result
                // this will throw an exception if none can be found
                SynchronizationProcessor.getDeviceClassByDiscoveryResult(discoveryResultDTO);
                // device class could be found so approve
                OpenHABRestCommunicator.getInstance().approve(discoveryResultDTO.thingUID, discoveryResultDTO.label);
            } catch (NotAvailableException ex) {
                // no matching device class found so ignore it for now
                LOGGER.warn("Ignore discovered thing[" + discoveryResultDTO.thingUID + "] because: " + ex.getMessage());
            }
        };
    }

    @Override
    public void activate() throws CouldNotPerformException {
        active = true;
        this.inboxAddedObservable.addDataObserver(observer);
        this.inboxUpdatedObservable.addDataObserver(observer);
        // trigger observer on device class changes, maybe this leads to new things that can be approved
        Registries.getClassRegistry().getDeviceClassRemoteRegistry().addDataObserver((source, data) -> {
            for (DiscoveryResultDTO discoveryResult : OpenHABRestCommunicator.getInstance().getDiscoveryResults()) {
                observer.update(null, discoveryResult);
            }
        });

        // perform an initial sync by iterating over all items already in the inbox
        try {
            for (DiscoveryResultDTO discoveryResultDTO : OpenHABRestCommunicator.getInstance().getDiscoveryResults()) {
                observer.update(null, discoveryResultDTO);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not perform initial sync of the openHAB inbox", ex);
        }
    }

    @Override
    public void deactivate() {
        this.inboxAddedObservable.removeDataObserver(observer);
        this.inboxUpdatedObservable.removeDataObserver(observer);
        active = false;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
