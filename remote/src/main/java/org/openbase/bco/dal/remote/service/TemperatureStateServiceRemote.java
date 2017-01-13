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
import org.openbase.bco.dal.lib.layer.service.collection.TemperatureStateProviderServiceCollection;
import org.openbase.bco.dal.lib.layer.service.provider.TemperatureStateProviderService;
import org.openbase.bco.dal.remote.unit.UnitRemote;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.TemperatureStateType.TemperatureState;
import rst.timing.TimestampType.Timestamp;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class TemperatureStateServiceRemote extends AbstractServiceRemote<TemperatureStateProviderService, TemperatureState> implements TemperatureStateProviderServiceCollection {

    public TemperatureStateServiceRemote() {
        super(ServiceType.TEMPERATURE_STATE_SERVICE);
    }

    @Override
    public Collection<TemperatureStateProviderService> getTemperatureStateProviderServices() {
        return getServices();
    }

    /**
     * {@inheritDoc}
     * Computes the average temperature value.
     *
     * @return {@inheritDoc}
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    protected TemperatureState computeServiceState() throws CouldNotPerformException {
        Double average = 0d;
        Collection<TemperatureStateProviderService> temperatureStateProviderServices = getTemperatureStateProviderServices();
        int amount = temperatureStateProviderServices.size();
        for (TemperatureStateProviderService provider : temperatureStateProviderServices) {
            if (!((UnitRemote) provider).isDataAvailable()) {
                amount--;
                continue;
            }

            average += provider.getTemperatureState().getTemperature();
        }
        average /= amount;

        return TemperatureState.newBuilder().setTemperature(average).setTimestamp(Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
    }

    @Override
    public TemperatureState getTemperatureState() throws NotAvailableException {
        return getServiceState();
    }
}
