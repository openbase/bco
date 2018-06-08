package org.openbase.bco.dal.remote.service;

/*
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
import org.openbase.bco.dal.lib.layer.service.collection.TamperStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.TamperStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TamperStateType.TamperState.Builder;
import rst.domotic.state.TamperStateType.TamperState.MapFieldEntry;
import rst.domotic.state.TamperStateType.TamperState;
import rst.domotic.state.TamperStateType.TamperState.State;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TamperStateServiceRemote extends AbstractServiceRemote<TamperStateProviderService, TamperState> implements TamperStateProviderServiceCollection {

    public TamperStateServiceRemote() {
        super(ServiceType.TAMPER_STATE_SERVICE, TamperState.class);
    }

    public Collection<TamperStateProviderService> getTamperStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the tamper state as tamper if at least one underlying service detects tamper and else no tamper.
     * Additionally the last detection timestamp as set as the latest of the underlying services.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected TamperState computeServiceState() throws CouldNotPerformException {
        return getTamperState(UnitType.UNKNOWN);
    }

    @Override
    public TamperState getTamperState() throws NotAvailableException {
        return getData();
    }

    @Override
    public TamperState getTamperState(final UnitType unitType) throws NotAvailableException {
        final Builder tamperStateBuilder = TamperState.newBuilder().setValue(State.NO_TAMPER);
        long timestamp = 0;

        for (TamperStateProviderService service : getServices(unitType)) {

            // do not handle if data is not synced yet.
            if (!((UnitRemote) service).isDataAvailable()) {
                continue;
            }

            // handle state
            TamperState tamperState = service.getTamperState();
            if (tamperState.getValue() == State.TAMPER) {
                tamperStateBuilder.setValue(TamperState.State.TAMPER);
            }

            // handle latest occurrence timestamps
            for (final MapFieldEntry entry : tamperState.getLastValueOccurrenceList()) {

                try {
                    ServiceStateProcessor.updateLatestValueOccurrence(entry.getKey(), entry.getValue(), tamperStateBuilder);
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could not update latest occurrence timestamp of Entry[" + entry + "]", ex, logger);
                }
            }

            // handle timestamp
            timestamp = Math.max(timestamp, tamperState.getTimestamp().getTime());
        }

        // update final timestamp
        TimestampProcessor.updateTimestamp(timestamp, tamperStateBuilder, TimeUnit.MICROSECONDS, logger);

        // return merged state
        return tamperStateBuilder.build();
    }
}
