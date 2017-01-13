package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
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
import org.openbase.bco.dal.lib.layer.service.provider.TemperatureStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.TemperatureStateType.TemperatureState;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface TemperatureStateProviderServiceCollection extends TemperatureStateProviderService {

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns the average temperature value for a collection of temperature
     * providers.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public TemperatureState getTemperatureState() throws NotAvailableException {

        try {
            Double average = 0d;
            for (TemperatureStateProviderService service : getTemperatureStateProviderServices()) {
                average += service.getTemperatureState().getTemperature();
            }
            average /= getTemperatureStateProviderServices().size();
            return TemperatureState.newBuilder().setTemperature(average).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Temperature", ex);
        }
    }

    public Collection<TemperatureStateProviderService> getTemperatureStateProviderServices() throws CouldNotPerformException;
}
