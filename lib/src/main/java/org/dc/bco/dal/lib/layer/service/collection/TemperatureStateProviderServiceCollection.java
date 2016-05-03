/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface TemperatureStateProviderServiceCollection extends TemperatureProviderService {

    /**
     * Returns the average temperature value for a collection of temperature
     * providers.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public Double getTemperature() throws NotAvailableException {

        try {
            Double average = 0d;
            for (TemperatureProviderService service : getTemperatureStateProviderServices()) {
                average += service.getTemperature();
            }
            average /= getTemperatureStateProviderServices().size();
            return average;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Temperature", ex);
        }
    }

    public Collection<TemperatureProviderService> getTemperatureStateProviderServices() throws CouldNotPerformException;
}
