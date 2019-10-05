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

import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.collection.EmphasisStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.EmphasisStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.TimestampProcessor;
import org.openbase.jul.schedule.FutureProcessor;
import org.openbase.type.domotic.action.ActionDescriptionType;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.service.ServiceTemplateType;
import org.openbase.type.domotic.state.EmphasisStateType.EmphasisState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class EmphasisStateServiceRemote extends AbstractServiceRemote<EmphasisStateOperationService, EmphasisState> implements EmphasisStateOperationServiceCollection {

    public EmphasisStateServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE, EmphasisState.class);
    }

    @Override
    protected EmphasisState computeServiceState() throws CouldNotPerformException {
        return getEmphasisState(UnitType.UNKNOWN);
    }

    @Override
    public Future<ActionDescriptionType.ActionDescription> setEmphasisState(final EmphasisState emphasisState, UnitType unitType) {
        try {
            return applyAction(ActionDescriptionProcessor.generateActionDescriptionBuilder(emphasisState, getServiceType(), unitType));
        } catch (CouldNotPerformException ex) {
            return FutureProcessor.canceledFuture(ActionDescription.class, ex);
        }
    }

    @Override
    public EmphasisState getEmphasisState(UnitType unitType) throws NotAvailableException {
        Collection<EmphasisStateOperationService> emphasisStateOperationServices = getServices(unitType);
        int amount = emphasisStateOperationServices.size();
        Double averageComfort = 0d;
        Double averageEconomy = 0d;
        Double averageSecurity = 0d;
        long timestamp = 0;
        for (EmphasisStateOperationService service : emphasisStateOperationServices) {
            if (!((UnitRemote) service).isDataAvailable()|| !(
                    service.getEmphasisState().hasComfort() ||
                    service.getEmphasisState().hasEconomy() ||
                    service.getEmphasisState().hasSecurity())) {
                amount--;
                continue;
            }

            if (amount == 0) {
                throw new NotAvailableException("EmphasisState");
            }

            averageComfort += service.getEmphasisState().getComfort();
            averageEconomy += service.getEmphasisState().getEconomy();
            averageSecurity += service.getEmphasisState().getSecurity();
            timestamp = Math.max(timestamp, service.getEmphasisState().getTimestamp().getTime());
        }
        averageComfort /= amount;
        averageEconomy /= amount;
        averageSecurity /= amount;
        return TimestampProcessor.updateTimestamp(timestamp, EmphasisState.newBuilder().setComfort(averageComfort).setEconomy(averageEconomy).setSecurity(averageSecurity), TimeUnit.MICROSECONDS, logger).build();
    }

}
