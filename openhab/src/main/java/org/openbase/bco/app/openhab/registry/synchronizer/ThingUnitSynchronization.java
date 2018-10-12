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

import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.thing.dto.ChannelDTO;
import org.eclipse.smarthome.core.thing.dto.ThingDTO;
import org.eclipse.smarthome.core.thing.link.dto.ItemChannelLinkDTO;
import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.registry.diff.IdentifiableEnrichedThingDTO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Synchronization for things managed by the bco binding.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ThingUnitSynchronization extends AbstractSynchronizer<String, IdentifiableEnrichedThingDTO> {

    /**
     * String identifying all things managed by the bco binding.
     */
    static final String BCO_BINDING_ID = "bco";

    public ThingUnitSynchronization(final SyncObject synchronizationLock) throws InstantiationException {
        super(new ThingObservable(), synchronizationLock);
    }

    @Override
    protected void afterInternalSync() {
        logger.info("Internal sync finished!");
    }

    @Override
    public void update(final IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Update {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        // validate items for thing, this is needed because channels for e.g. locations can change during runtime e.g. because they contain new units
        registerAndValidateItems(identifiableEnrichedThingDTO.getDTO());
        // update unit label and location if needed
        final UnitConfig.Builder unitConfig = Registries.getUnitRegistry().getUnitConfigById(getUnitId(identifiableEnrichedThingDTO.getDTO())).toBuilder();
        if (SynchronizationProcessor.updateUnitToThing(identifiableEnrichedThingDTO.getDTO(), unitConfig)) {
            Registries.getUnitRegistry().updateUnitConfig(unitConfig.build());
//            try {
//                Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();
//            } catch (ExecutionException ex) {
//                throw new CouldNotPerformException("Could not update device[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] for thing[" + identifiableEnrichedThingDTO.getId() + "]", ex);
//            }
        }
    }

    @Override
    public void register(final IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException {
        logger.info("Register {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        registerAndValidateItems(identifiableEnrichedThingDTO.getDTO());
    }

    @Override
    public void remove(final IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        logger.info("Remove {} ...", identifiableEnrichedThingDTO.getDTO().UID);
        final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(getUnitId(identifiableEnrichedThingDTO.getDTO()));
        Registries.getUnitRegistry().removeUnitConfig(unitConfig);
//        try {
//            Registries.getUnitRegistry().removeUnitConfig(unitConfig).get();
//        } catch (ExecutionException ex) {
//            throw new CouldNotPerformException("Could not remove unit " + unitConfig.getAlias(0), ex);
//        }
    }

    @Override
    public List<IdentifiableEnrichedThingDTO> getEntries() throws CouldNotPerformException {
        final List<IdentifiableEnrichedThingDTO> identifiableEnrichedThingDTOList = new ArrayList<>();
        for (final EnrichedThingDTO enrichedThingDTO : OpenHABRestCommunicator.getInstance().getThings()) {
            identifiableEnrichedThingDTOList.add(new IdentifiableEnrichedThingDTO(enrichedThingDTO));
        }
        return identifiableEnrichedThingDTOList;
    }

    @Override
    public boolean isSupported(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) {
        // only handle things of the bco binding
        return identifiableEnrichedThingDTO.getId().startsWith(BCO_BINDING_ID);
    }

    private String getUnitId(ThingDTO thingDTO) {
        String[] split = thingDTO.UID.split(":");
        return split[split.length - 1];
    }

    private void registerAndValidateItems(final ThingDTO thingDTO) throws CouldNotPerformException {
        // save current item channel links to compute if some already exist
        final List<ItemChannelLinkDTO> itemChannelLinks = OpenHABRestCommunicator.getInstance().getItemChannelLinks();
        // retrieve the unit belonging to the thing
        final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(getUnitId(thingDTO));
        // iterate over all channels
        for (final ChannelDTO channel : thingDTO.channels) {
            // retrieve service type for the channel and generate item name
            ServiceType serviceType;
            String itemName;
            String itemLabel;
            if (channel.id.equals("power_state_light")) {
                // handle special case fo location item
                serviceType = ServiceType.POWER_STATE_SERVICE;
                itemName = OpenHABItemProcessor.generateItemName(unitConfig, serviceType) + "Light";
                itemLabel = SynchronizationProcessor.generateItemLabel(unitConfig, serviceType) + " Light";
            } else {
                serviceType = ServiceType.valueOf(channel.id.toUpperCase() + "_SERVICE");
                itemName = OpenHABItemProcessor.generateItemName(unitConfig, serviceType);
                itemLabel = SynchronizationProcessor.generateItemLabel(unitConfig, serviceType);
            }

            // if item does not already exist register it
            if (!OpenHABRestCommunicator.getInstance().hasItem(itemName)) {
                ItemDTO itemDTO = new ItemDTO();
                itemDTO.name = itemName;
                itemDTO.label = itemLabel;
                try {
                    itemDTO.type = OpenHABItemProcessor.getItemType(serviceType);
                } catch (NotAvailableException ex) {
                    logger.warn("Skip registration of item for service {} of unit {} because no item type available", serviceType.name(), unitConfig.getAlias(0));
                    continue;
                }
                OpenHABRestCommunicator.getInstance().registerItem(itemDTO);
            }

            // compute if channel link for the item already exist
            boolean hasItemChannelLink = false;
            for (ItemChannelLinkDTO itemChannelLink : itemChannelLinks) {
                if (itemChannelLink.itemName.equals(itemName) && itemChannelLink.channelUID.equals(channel.uid)) {
                    hasItemChannelLink = true;
                    break;
                }
            }

            // if link does not exist register it
            if (!hasItemChannelLink) {
                OpenHABRestCommunicator.getInstance().registerItemChannelLink(itemName, channel.uid);
            }
        }
    }
}
