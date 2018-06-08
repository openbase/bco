package org.openbase.bco.dal.remote.service;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor;
import org.openbase.bco.dal.lib.layer.service.collection.PresenceStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.PresenceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PresenceStateType.PresenceState;
import rst.domotic.state.PresenceStateType.PresenceState.Builder;
import rst.domotic.state.PresenceStateType.PresenceState.MapFieldEntry;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class PresenceStateServiceRemote extends AbstractServiceRemote<PresenceStateProviderService, PresenceState> implements PresenceStateProviderServiceCollection {

    public PresenceStateServiceRemote() {
        super(ServiceType.PRESENCE_STATE_SERVICE, PresenceState.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected PresenceState computeServiceState() throws CouldNotPerformException {
        return getPresenceState(UnitType.UNKNOWN);
    }

    @Override
    public PresenceState getPresenceState() throws NotAvailableException {
        return getData();
    }

    @Override
    public PresenceState getPresenceState(final UnitType unitType) throws NotAvailableException {
        final Builder presenceStateBuilder = PresenceState.newBuilder().setValue(PresenceState.State.ABSENT);
        long timestamp = 0;

        for (PresenceStateProviderService service : getServices(unitType)) {

            // do not handle if data is not synced yet.
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            // handle state
            PresenceState presenceState = service.getPresenceState();
            if (presenceState.getValue() == PresenceState.State.PRESENT) {
                presenceStateBuilder.setValue(PresenceState.State.PRESENT);
            }

            // handle latest occurrence timestamps
            for (final MapFieldEntry entry : presenceState.getLastValueOccurrenceList()) {

                try {
                    ServiceStateProcessor.updateLatestValueOccurrence(entry.getKey(), entry.getValue(), presenceStateBuilder);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update latest occurrence timestamp of Entry[" + entry + "]", ex, logger);
                }
            }

            // handle timestamp
            timestamp = Math.max(timestamp, presenceState.getTimestamp().getTime());
        }

        // update final timestamp
        TimestampProcessor.updateTimestamp(timestamp, presenceStateBuilder, TimeUnit.MICROSECONDS, logger);

        // return merged state
        return presenceStateBuilder.build();
    }

    public Collection<PresenceStateProviderService> getPresenceStateProviderServices() throws CouldNotPerformException {
        return getServices();
    }
}
