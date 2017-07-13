package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.collection.BlindStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.BlindStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.BlindStateType.BlindState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BlindStateServiceRemote extends AbstractServiceRemote<BlindStateOperationService, BlindState> implements BlindStateOperationServiceCollection {

    public BlindStateServiceRemote() {
        super(ServiceType.BLIND_STATE_SERVICE, BlindState.class);
    }

    public Collection<BlindStateOperationService> getBlindStateOperationServices() {
        return getServices();
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
    public BlindState getBlindState() throws NotAvailableException {
        return getData();
    }

    // TODO: das filtern nach dem unit typen fehlt noch...
    @Override
    public BlindState getBlindState(UnitType unitType) throws NotAvailableException {
        int serviceNumber = getBlindStateOperationServices().size(), stop = 0, down = 0, up = 0;
        long timestamp = 0;
        float openingRatioAverage = 0;
        for (BlindStateOperationService service : getBlindStateOperationServices()) {
            if (!((UnitRemote) service).isDataAvailable()) {
                serviceNumber--;
                continue;
            }

            switch (service.getBlindState().getMovementState()) {
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

        openingRatioAverage /= serviceNumber;
        BlindState.MovementState mostOccurences;
        if (stop >= up && stop >= down) {
            mostOccurences = BlindState.MovementState.STOP;
        } else if (up >= stop && up >= down) {
            mostOccurences = BlindState.MovementState.UP;
        } else {
            mostOccurences = BlindState.MovementState.DOWN;
        }

        return TimestampProcessor.updateTimestamp(timestamp, BlindState.newBuilder().setMovementState(mostOccurences).setOpeningRatio(openingRatioAverage), TimeUnit.MICROSECONDS, logger).build();
    }

    @Override
    public Future<ActionFuture> setBlindState(final BlindState blindState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);

        try {
            return applyAction(Service.upateActionDescription(actionDescription, blindState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set blindState", ex);
        }
    }

    @Override
    public Future<ActionFuture> setBlindState(final BlindState blindState, final UnitType unitType) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitType(unitType);
        
        try {
            return applyAction(Service.upateActionDescription(actionDescription, blindState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set blindState", ex);
        }
    }
}
