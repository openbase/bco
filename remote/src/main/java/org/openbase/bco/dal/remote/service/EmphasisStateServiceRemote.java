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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.Service;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.service.collection.EmphasisStateOperationServiceCollection;
import org.openbase.bco.dal.lib.layer.service.operation.EmphasisStateOperationService;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.ActionDescriptionProcessor;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionAuthorityType.ActionAuthority;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType;
import rst.domotic.state.EmphasisStateType.EmphasisState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:tmichalski@techfak.uni-bielefeld.de">Timo Michalski</a>
 */
public class EmphasisStateServiceRemote extends AbstractServiceRemote<EmphasisStateOperationService, EmphasisState> implements EmphasisStateOperationServiceCollection {

    public EmphasisStateServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.EMPHASIS_STATE_SERVICE, EmphasisState.class);
    }

    public Collection<EmphasisStateOperationService> getEmphasisStateOperationServices() throws CouldNotPerformException {
        return getServices();
    }

    @Override
    protected EmphasisState computeServiceState() throws CouldNotPerformException {
        return getEmphasisState(UnitType.UNKNOWN);
    }

    @Override
    public Future<ActionFutureType.ActionFuture> setEmphasisState(EmphasisState emphasisState) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);

        try {
            return applyAction(Services.updateActionDescription(actionDescription, emphasisState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set brightnessState", ex);
        }
    }

    @Override
    public Future<ActionFutureType.ActionFuture> setEmphasisState(EmphasisState emphasisState, UnitType unitType) throws CouldNotPerformException {
        ActionDescription.Builder actionDescription = ActionDescriptionProcessor.getActionDescription(ActionAuthority.getDefaultInstance(), ResourceAllocation.Initiator.SYSTEM);
        ServiceStateDescription.Builder serviceStateDescription = actionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitType(unitType);

        try {
            return applyAction(Services.updateActionDescription(actionDescription, emphasisState, getServiceType()).build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CouldNotPerformException("Could not set brightnessState", ex);
        }
    }

    @Override
    public EmphasisState getEmphasisState() throws NotAvailableException {
        return getData();
    }

    @Override
    public EmphasisState getEmphasisState(UnitType unitType) throws NotAvailableException {
        Collection<EmphasisStateOperationService> emphasisStateOperationServices = getServices(unitType);
        int serviceNumber = emphasisStateOperationServices.size();
        Double averageComfort = 0d;
        Double averageEnergy = 0d;
        Double averageSecurity = 0d;
        long timestamp = 0;
        for (EmphasisStateOperationService service : emphasisStateOperationServices) {
            if (!((UnitRemote) service).isDataAvailable()) {
                serviceNumber--;
                continue;
            }
            averageComfort += service.getEmphasisState().getComfort();
            averageEnergy += service.getEmphasisState().getEnergy();
            averageSecurity += service.getEmphasisState().getSecurity();
            timestamp = Math.max(timestamp, service.getEmphasisState().getTimestamp().getTime());
        }
        averageComfort /= serviceNumber;
        averageEnergy /= serviceNumber;
        averageSecurity /= serviceNumber;
        return TimestampProcessor.updateTimestamp(timestamp, EmphasisState.newBuilder().setComfort(averageComfort).setEnergy(averageEnergy).setSecurity(averageSecurity), TimeUnit.MICROSECONDS, logger).build();
    }

}
