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
import org.openbase.bco.app.openhab.registry.diff.IdentifiableEnrichedItemDTO;
import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemHelper.OpenHABItemNameMetaData;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ItemDalUnitSynchronization extends AbstractSynchronizer<String, IdentifiableEnrichedItemDTO> {

    public ItemDalUnitSynchronization(final SyncObject synchronizationLock) throws InstantiationException {
        super(new ItemObservable(), synchronizationLock);
    }

    @Override
    public void update(final IdentifiableEnrichedItemDTO identifiableEnrichedItemDTO) throws CouldNotPerformException {
        logger.info("Update item[" + identifiableEnrichedItemDTO.getId() + "]");
        validateAndUpdateItem(identifiableEnrichedItemDTO.getDTO());
    }

    @Override
    public void register(final IdentifiableEnrichedItemDTO identifiableEnrichedItemDTO) throws CouldNotPerformException {
        logger.info("Register item[" + identifiableEnrichedItemDTO.getId() + "]");
        // do nothing because items are registers by the thing synchronization
        validateAndUpdateItem(identifiableEnrichedItemDTO.getDTO());
    }

    @Override
    public void remove(final IdentifiableEnrichedItemDTO identifiableEnrichedItemDTO) throws CouldNotPerformException {
        logger.info("Remove item[" + identifiableEnrichedItemDTO.getId() + "]");
        final String alias = new OpenHABItemNameMetaData(identifiableEnrichedItemDTO.getId()).getAlias();

        try {
            // unit exists for item so sync and register it again
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigByAlias(alias);
            logger.info("Re-register item");
            updateItem(unitConfig, identifiableEnrichedItemDTO.getDTO());
            OpenHABRestCommunicator.getInstance().registerItem(identifiableEnrichedItemDTO.getDTO());
        } catch (NotAvailableException ex) {
            // unit does not exist so removal is okay
        }
    }

    @Override
    public List<IdentifiableEnrichedItemDTO> getEntries() throws CouldNotPerformException {
        final List<IdentifiableEnrichedItemDTO> itemList = new ArrayList<>();
        for (final EnrichedItemDTO item : OpenHABRestCommunicator.getInstance().getItems()) {
            itemList.add(new IdentifiableEnrichedItemDTO(item));
        }
        return itemList;
    }

    @Override
    public boolean verifyEntry(final IdentifiableEnrichedItemDTO identifiableEnrichedItemDTO) {
        return true;
    }

    private boolean updateItem(final UnitConfig unitConfig, final EnrichedItemDTO item) throws NotAvailableException {
        boolean modification = false;
        final String label = LabelProcessor.getBestMatch(unitConfig.getLabel());
        if (item.label == null || !item.label.equals(label)) {
            item.label = label;
            modification = true;
        }

        return modification;
    }

    private void validateAndUpdateItem(final EnrichedItemDTO item) throws CouldNotPerformException {
        final String alias = new OpenHABItemNameMetaData(item.name).getAlias();

        try {
            // unit exists for item so sync label from dal unit back to item if necessary
            final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigByAlias(alias);
            logger.info("Found unit config for item [" + item.name + "]");
            if (updateItem(unitConfig, item)) {
                OpenHABRestCommunicator.getInstance().updateItem(item);
            }
        } catch (NotAvailableException ex) {
            // unit does not exist for item so remove it
            logger.info("Delete item[" + item.name + "]");
            try {
                OpenHABRestCommunicator.getInstance().deleteItem(item);
            } catch (CouldNotPerformException exx) {
                // It seems like openHAB is sometimes not deleting item channel links but the links still
                // cause items to be returned when queried.
                // Thus if the item could not be deleted search a link still referencing it.
                for (final ItemChannelLinkDTO itemChannelLink : OpenHABRestCommunicator.getInstance().getItemChannelLinks()) {
                    if (itemChannelLink.itemName.equals(item.name)) {
                        OpenHABRestCommunicator.getInstance().deleteItemChannelLink(itemChannelLink);
                        return;
                    }
                }
                throw exx;
            }
        }
    }
}
