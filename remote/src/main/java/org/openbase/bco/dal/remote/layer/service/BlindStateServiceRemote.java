package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.collection.BlindStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BlindStateType.BlindState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BlindStateServiceRemote extends AbstractServiceRemote<BlindStateOperationService, BlindState> implements BlindStateOperationServiceCollection {

    public BlindStateServiceRemote() {
        super(ServiceType.BLIND_STATE_SERVICE, BlindState.class);
    }

    /**
     * {@inheritDoc} Computes the average opening ratio and the movement state which appears the most.
     *
     * @return
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected BlindState computeServiceState() throws CouldNotPerformException {
        return getBlindState(UnitType.UNKNOWN);
    }

    @Override
    public BlindState getBlindState(UnitType unitType) throws NotAvailableException {

        final Collection<BlindStateOperationService> blindStateOperationServiceCollection = getServices(unitType);
        int amount = blindStateOperationServiceCollection.size(), stop = 0, down = 0, up = 0;
        long timestamp = 0;
        float openingRatioAverage = 0;
        for (BlindStateOperationService service : blindStateOperationServiceCollection) {
            if (!((UnitRemote) service).isDataAvailable() || !service.getBlindState().hasValue()) {
                amount--;
                continue;
            }

            if (amount == 0) {
                throw new NotAvailableException("BlindState");
            }

            switch (service.getBlindState().getValue()) {
                case DOWN:
                    down++;
                    break;
                case STOP:
                    stop++;
                    break;
                case UP:
                    up++;
                    break;
            }

            openingRatioAverage += service.getBlindState().getOpeningRatio();
            timestamp = Math.max(timestamp, service.getBlindState().getTimestamp().getTime());
        }

        openingRatioAverage /= amount;
        BlindState.State mostOccurrences;
        if (stop >= up && stop >= down) {
            mostOccurrences = BlindState.State.STOP;
        } else if (up >= stop && up >= down) {
            mostOccurrences = BlindState.State.UP;
        } else {
            mostOccurrences = BlindState.State.DOWN;
        }

        return TimestampProcessor.updateTimestamp(timestamp, BlindState.newBuilder().setValue(mostOccurrences).setOpeningRatio(openingRatioAverage), TimeUnit.MICROSECONDS, logger).build();
    }

    @Override
    public Future<ActionDescription> setBlindState(final BlindState blindState, final UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(blindState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }
}
