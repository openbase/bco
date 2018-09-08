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

import org.eclipse.smarthome.io.rest.core.thing.EnrichedThingDTO;
import org.openbase.bco.app.openhab.OpenHABRestCommunicator;
import org.openbase.bco.app.openhab.registry.diff.IdentifiableEnrichedThingDTO;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.schedule.SyncObject;
import org.openbase.jul.storage.registry.AbstractSynchronizer;
import rst.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Synchronization for things managed by the bco binding.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ThingBCOUnitSynchronization extends AbstractSynchronizer<String, IdentifiableEnrichedThingDTO> {

    static final String BCO_BINDING_ID = "bco";

    public ThingBCOUnitSynchronization(final SyncObject synchronizationLock) throws InstantiationException {
        super(new ThingObservable(), synchronizationLock);
    }

    @Override
    public void update(final IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        final EnrichedThingDTO updatedThing = identifiableEnrichedThingDTO.getDTO();
        // get unit config for the thing
        final UnitConfig.Builder unitConfig = Registries.getUnitRegistry().getUnitConfigById(getUnitId(identifiableEnrichedThingDTO)).toBuilder();

        if (SynchronizationProcessor.updateUnitToThing(updatedThing, unitConfig)) {
            try {
                Registries.getUnitRegistry().updateUnitConfig(unitConfig.build()).get();
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not update device[" + LabelProcessor.getBestMatch(unitConfig.getLabel()) + "] for thing[" + identifiableEnrichedThingDTO.getId() + "]", ex);
            }
        }
    }

    @Override
    public void register(final IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException {
        // extract the unit id from the thing id
        final String unitId = getUnitId(identifiableEnrichedThingDTO);
        SynchronizationProcessor.registerAndValidateItems(Registries.getUnitRegistry().getUnitConfigById(unitId), identifiableEnrichedThingDTO.getDTO());
    }

    @Override
    public void remove(final IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws CouldNotPerformException, InterruptedException {
        final String unitId = getUnitId(identifiableEnrichedThingDTO);
        final UnitConfig unitConfig = Registries.getUnitRegistry().getUnitConfigById(unitId);
        try {
            Registries.getUnitRegistry().registerUnitConfig(unitConfig).get();
        } catch (ExecutionException ex) {
            throw new CouldNotPerformException("Could not remote unit [" + unitConfig.getAlias(0) + "]", ex);
        }
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
    public boolean verifyEntry(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) throws VerificationFailedException {
        // only handle things of the bco binding
        return identifiableEnrichedThingDTO.getId().startsWith(BCO_BINDING_ID);
    }

    private String getUnitId(IdentifiableEnrichedThingDTO identifiableEnrichedThingDTO) {
        String[] split = identifiableEnrichedThingDTO.getId().split(":");
        return split[split.length - 1];
    }
}
