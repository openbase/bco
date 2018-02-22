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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.BrightnessStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
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
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

// TODO pleminoq: This seems to cause in problems because units using this service in different ways.
/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class BrightnessStateServiceRemote extends AbstractServiceRemote<BrightnessStateOperationService, BrightnessState> implements BrightnessStateOperationServiceCollection {

    public BrightnessStateServiceRemote() {
        super(ServiceType.BRIGHTNESS_STATE_SERVICE, BrightnessState.class);
    }

    public Collection<BrightnessStateOperationService> getBrightnessStateOperationServices() throws CouldNotPerformException {
        return getServices();
    }

    @Override
    public Future<ActionFuture> setBrightnessState(final BrightnessState brightnessState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);

        try {
            return applyAction(Services.updateActionDescription(actionDescription, brightnessState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set brightnessState", ex);
        }
    }

    @Override
    public Future<ActionFuture> setBrightnessState(final BrightnessState brightnessState, final UnitType unitType) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitType(unitType);
        
        try {
            return applyAction(Services.updateActionDescription(actionDescription, brightnessState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set brightnessState", ex);
        }
    }

    /**
     * {@inheritDoc}
     * Computes the average brightness value.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected BrightnessState computeServiceState() throws CouldNotPerformException {
        return getBrightnessState(UnitType.UNKNOWN);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        return getData();
    }

    @Override
    public BrightnessState getBrightnessState(final UnitType unitType) throws NotAvailableException {
        Collection<BrightnessStateOperationService> brightnessStateOperationServices = getServices(unitType);
        int serviceNumber = brightnessStateOperationServices.size();
        Double average = 0d;
        long timestamp = 0;
        for (BrightnessStateOperationService service : brightnessStateOperationServices) {
            if (!((UnitRemote) service).isDataAvailable()) {
                serviceNumber--;
                continue;
            }
            average += service.getBrightnessState().getBrightness();
            timestamp = Math.max(timestamp, service.getBrightnessState().getTimestamp().getTime());
        }
        average /= serviceNumber;
        return TimestampProcessor.updateTimestamp(timestamp, BrightnessState.newBuilder().setBrightness(average), TimeUnit.MICROSECONDS, logger).build();
    }
}
