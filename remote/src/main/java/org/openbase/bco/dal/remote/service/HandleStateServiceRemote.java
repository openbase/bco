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
import java.util.concurrent.TimeUnit;
import org.openbase.bco.dal.lib.layer.service.collection.HandleStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.HandleStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.HandleStateType.HandleState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class HandleStateServiceRemote extends AbstractServiceRemote<HandleStateProviderService, HandleState> implements HandleStateProviderServiceCollection {

    public HandleStateServiceRemote() {
        super(ServiceType.HANDLE_STATE_SERVICE, HandleState.class);
    }

    public Collection<HandleStateProviderService> getHandleStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected HandleState computeServiceState() throws CouldNotPerformException {
        return getHandleState(UnitType.UNKNOWN);
    }

    @Override
    public HandleState getHandleState() throws NotAvailableException {
        return getServiceState();
    }

    @Override
    public HandleState getHandleState(final UnitType unitType) throws NotAvailableException {
        // TODO: rethink position in handle state
        int position = 0;
        //boolean tilted = false;
        Collection<HandleStateProviderService> handleStateProviderServices = getServices(unitType);
        int amount = handleStateProviderServices.size();
        long timestamp = 0;
        for (HandleStateProviderService service : handleStateProviderServices) {
            if (!((UnitRemote) service).isDataAvailable()) {
                amount--;
                continue;
            }
            position += service.getHandleState().getPosition();
            timestamp = Math.max(timestamp, service.getHandleState().getTimestamp().getTime());
        }

        position /= amount;
        return TimestampProcessor.updateTimestamp(timestamp, HandleState.newBuilder().setPosition(position), TimeUnit.MICROSECONDS, logger).build();
    }
}
