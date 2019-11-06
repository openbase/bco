/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.remote.layer.service;

/*-
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

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.openbase.bco.dal.lib.layer.service.collection.IlluminanceStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.BrightnessStateType.BrightnessState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState;
import org.openbase.type.domotic.state.IlluminanceStateType.IlluminanceState.Builder;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 * @author pleminoq
 */
public class IlluminanceStateServiceRemote extends AbstractServiceRemote<IlluminanceStateProviderService, IlluminanceState> implements IlluminanceStateProviderServiceCollection {

    public IlluminanceStateServiceRemote() {
        super(ServiceType.ILLUMINANCE_STATE_SERVICE, IlluminanceState.class);
    }

    /**
     * {@inheritDoc}
     * Computes the average current and voltage and the sum of the consumption of the underlying services.
     *
     * @return {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected IlluminanceState computeServiceState() throws CouldNotPerformException {
        return getIlluminanceState(UnitType.UNKNOWN);
    }

    @Override
    public IlluminanceState getIlluminanceState(final UnitType unitType) throws NotAvailableException {

        // prepare fields
        double averageIlluminance = 0;
        long timestamp = 0;
        Collection<IlluminanceStateProviderService> illuminanceStateProviderServices = getServices(unitType);
        int amount = illuminanceStateProviderServices.size();
        ActionDescription latestAction = null;

        // iterate over all services and collect available states
        for (IlluminanceStateProviderService service : illuminanceStateProviderServices) {
            final IlluminanceState state = service.getIlluminanceState();

            if (!((UnitRemote) service).isDataAvailable() || !state.hasIlluminance()) {
                amount--;
                continue;
            }

            averageIlluminance += state.getIlluminance();
            timestamp = Math.max(timestamp, state.getTimestamp().getTime());

            // select latest action
            latestAction = selectLatestAction(state, latestAction);
        }

        if (amount == 0) {
            throw new NotAvailableException("IlluminanceState");
        }

        // finally compute average
        averageIlluminance = averageIlluminance / amount;

        // setup state
        final Builder serviceStateBuilder = IlluminanceState.newBuilder().setIlluminance(averageIlluminance).setIlluminanceDataUnit(IlluminanceState.DataUnit.LUX);

        // setup timestamp
        TimestampProcessor.updateTimestamp(timestamp, serviceStateBuilder, TimeUnit.MICROSECONDS, logger).build();

        // setup responsible action with latest action as cause.
        setupResponsibleActionForNewAggregatedServiceState(serviceStateBuilder, latestAction);

        return serviceStateBuilder.build();
    }
}
