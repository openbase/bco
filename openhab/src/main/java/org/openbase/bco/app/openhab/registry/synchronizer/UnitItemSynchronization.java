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

import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.io.rest.core.item.EnrichedItemDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.IdentifiableMessage;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitConfigType.UnitConfig.Builder;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class managing the synchronization from BCO DAL units to openHAB items.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitItemSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, Builder>> {

    /**
     * Create a new synchronization manager BCO DAL units to openHAB items.
     *
     * @param synchronizationLock a lock used during a synchronization. This is lock should be shared with
     *                            other synchronization managers so that they will not interfere with each other.
     * @throws InstantiationException if instantiation fails
     * @throws NotAvailableException  if the unit registry is not available
     */
    public UnitItemSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getUnitConfigRemoteRegistry(), synchronizationLock);
    }

    /**
     * Validate that all items for a dal unit exist and update them. If they do not exist they will be registered.
     *
     * @param identifiableMessage the dal unit updated
     * @throws CouldNotPerformException if the items could not be updated or registered
     */
    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // validate that all items for a dal unit exist
        final UnitConfig unitConfig = identifiableMessage.getMessage();
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            final ServiceType serviceType = serviceConfig.getServiceDescription().getServiceType();
            if (serviceTypeSet.contains(serviceType)) {
                continue;
            }
            serviceTypeSet.add(serviceType);

            final String itemName = OpenHABItemProcessor.generateItemName(identifiableMessage.getMessage(), serviceType);
            try {
                // item exists, update if necessary
                final EnrichedItemDTO item = OpenHABRestCommunicator.getInstance().getItem(itemName);
                if (updateItem(unitConfig, item)) {
                    OpenHABRestCommunicator.getInstance().updateItem(item);
                }
            } catch (NotAvailableException ex) {
                // item does not exist so register them
                SynchronizationProcessor.registerAndValidateItems(unitConfig);
            }
        }
    }

    /**
     * Only perform an initial sync between a dal unit and its items by calling {@link #update(IdentifiableMessage)}
     * if its the initial sync.
     *
     * @param identifiableMessage the dal unit initially registered
     * @throws CouldNotPerformException if the initial update could not be performed.
     */
    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // make sure that after startup an initial synchronization is performed
        if (isInitialSync()) {
            update(identifiableMessage);
        }
        // do nothing, should be handled when device is added via openHAB
    }

    /**
     * Remove items and their channel links belonging to the removed dal unit.
     *
     * @param identifiableMessage the removed dal unit
     * @throws CouldNotPerformException if the items and channel links could not be removed
     */
    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        // remove items and channel links
        final UnitConfig unitConfig = identifiableMessage.getMessage();
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            final ServiceType serviceType = serviceConfig.getServiceDescription().getServiceType();
            if (serviceTypeSet.contains(serviceType)) {
                continue;
            }
            serviceTypeSet.add(serviceType);

            final String itemName = OpenHABItemProcessor.generateItemName(identifiableMessage.getMessage(), serviceType);
            try {
                OpenHABRestCommunicator.getInstance().getItem(itemName);
            } catch (NotAvailableException ex) {
                // do nothing because item does not exist
                continue;
            }

            // remove item
            OpenHABRestCommunicator.getInstance().deleteItem(itemName);
            // remove links
            for (final ItemChannelLinkDTO itemChannelLink : OpenHABRestCommunicator.getInstance().getItemChannelLinks()) {
                if (itemChannelLink.itemName.equals(itemName)) {
                    OpenHABRestCommunicator.getInstance().deleteItemChannelLink(itemChannelLink);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of dal units managed by the unit registry
     * @throws CouldNotPerformException it the dal units are not available
     */
    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getUnitConfigRemoteRegistry().getEntries();
    }

    /**
     * Verify that the dal unit is hosted by a device which is managed by openHAB.
     *
     * @param identifiableMessage the dal unit checked
     * @return if the unit is hosted by a device managed by openHAB
     * @throws VerificationFailedException if he verification failed
     */
    @Override
    public boolean verifyEntry(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws VerificationFailedException {
        switch (identifiableMessage.getMessage().getUnitType()) {
            case DEVICE:
                // retrieve host unit
                final UnitConfig hostUnit;
                if (identifiableMessage.getMessage().getUnitHostId().isEmpty()) {
                    return false;
                }
                try {
                    hostUnit = Registries.getUnitRegistry().getUnitConfigById(identifiableMessage.getMessage().getUnitHostId());
                } catch (CouldNotPerformException ex) {
                    throw new VerificationFailedException("Could not verify id dal unit[" + identifiableMessage.getMessage().getAlias(0) + "] is managed by openHAB", ex);
                }

                // validate that host is a device
                if (hostUnit.getUnitType() != UnitType.DEVICE) {
                    return false;
                }

                // validate that the device is configured via openHAB
                try {
                    SynchronizationProcessor.getThingIdFromDevice(hostUnit);
                    return true;
                } catch (NotAvailableException ex) {
                    return false;
                }
            default:
                try {
                    SynchronizationProcessor.getThingForUnit(identifiableMessage.getMessage());
                    return true;
                } catch (NotAvailableException ex) {
                    return false;
                }
        }
    }

    /**
     * Update the item label to a dal unit.
     *
     * @param unitConfig the dal unit from which the label
     * @param item       the item which is updated
     * @return if the item has been updated meaning that the label changed
     * @throws NotAvailableException if no label is available for the dal unit
     */
    private boolean updateItem(final UnitConfig unitConfig, final EnrichedItemDTO item) throws NotAvailableException {
        boolean modification = false;
        final String label = LabelProcessor.getBestMatch(unitConfig.getLabel());
        if (!item.label.equals(label)) {
            item.label = label;
            modification = true;
        }

        return modification;
    }
}
