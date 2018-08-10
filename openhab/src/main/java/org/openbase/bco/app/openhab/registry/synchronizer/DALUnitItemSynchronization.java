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
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DALUnitItemSynchronization extends AbstractSynchronizer<String, IdentifiableMessage<String, UnitConfig, Builder>> {

    public DALUnitItemSynchronization(final SyncObject synchronizationLock) throws InstantiationException, NotAvailableException {
        super(Registries.getUnitRegistry().getDalUnitConfigRemoteRegistry(), synchronizationLock);
    }

    @Override
    public void update(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.info("Update dal unit[" + identifiableMessage.getMessage().getAlias(0) + "]");
        final UnitConfig unitConfig = identifiableMessage.getMessage();
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            final ServiceType serviceType = serviceConfig.getServiceDescription().getServiceType();
            if (serviceTypeSet.contains(serviceType)) {
                continue;
            }
            serviceTypeSet.add(serviceType);

            final String itemName = OpenHABItemHelper.generateItemName(identifiableMessage.getMessage(), serviceType);
            try {
                final EnrichedItemDTO item = OpenHABRestCommunicator.getInstance().getItem(itemName);
                if (updateItem(unitConfig, item)) {
                    logger.info("Update item [" + itemName + "]");
                    OpenHABRestCommunicator.getInstance().updateItem(item);
                }
            } catch (NotAvailableException ex) {
                SynchronizationHelper.registerAndValidateItems(unitConfig);
            }
        }
    }

    @Override
    public void register(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.info("Register dal unit[" + identifiableMessage.getMessage().getAlias(0) + "]");
        if (isInitialSync()) {
            update(identifiableMessage);
        }
        // do nothing, should be handled when device is added via openHAB
    }

    @Override
    public void remove(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws CouldNotPerformException {
        logger.info("Remove dal unit[" + identifiableMessage.getMessage().getAlias(0) + "]");
        // remove items and channel links
        final UnitConfig unitConfig = identifiableMessage.getMessage();
        final Set<ServiceType> serviceTypeSet = new HashSet<>();
        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
            final ServiceType serviceType = serviceConfig.getServiceDescription().getServiceType();
            if (serviceTypeSet.contains(serviceType)) {
                continue;
            }
            serviceTypeSet.add(serviceType);

            final String itemName = OpenHABItemHelper.generateItemName(identifiableMessage.getMessage(), serviceType);
            try {
                OpenHABRestCommunicator.getInstance().getItem(itemName);
            } catch (NotAvailableException ex) {
                // do nothing because item does not exist
                continue;
            }

            logger.info("Delete item[" + itemName + "] and its channel links");
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

    @Override
    public List<IdentifiableMessage<String, UnitConfig, Builder>> getEntries() throws CouldNotPerformException {
        return Registries.getUnitRegistry().getDalUnitConfigRemoteRegistry().getEntries();
    }

    @Override
    public boolean verifyEntry(final IdentifiableMessage<String, UnitConfig, Builder> identifiableMessage) throws VerificationFailedException {
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
            SynchronizationHelper.getThingIdFromDevice(hostUnit);
            return true;
        } catch (NotAvailableException ex) {
            return false;
        }
    }

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
