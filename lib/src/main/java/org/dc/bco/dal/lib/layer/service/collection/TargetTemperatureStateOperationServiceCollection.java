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
import java.util.concurrent.Future;
import org.dc.bco.dal.lib.layer.service.operation.TargetTemperatureOperationService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.schedule.GlobalExecutionService;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface TargetTemperatureStateOperationServiceCollection extends TargetTemperatureOperationService {

    @Override
    default public Future<Void> setTargetTemperature(Double value) throws CouldNotPerformException {
        return GlobalExecutionService.allOf((TargetTemperatureOperationService input) -> input.setTargetTemperature(value), getTargetTemperatureStateOperationServices());
    }

    /**
     * Returns the average target temperature value for a collection of target
     * temperature services.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public Double getTargetTemperature() throws NotAvailableException {
        try {
            Double average = 0d;
            for (TargetTemperatureOperationService service : getTargetTemperatureStateOperationServices()) {
                average += service.getTargetTemperature();
            }
            average /= getTargetTemperatureStateOperationServices().size();
            return average;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("TargetTemperature", ex);
        }
    }

    public Collection<TargetTemperatureOperationService> getTargetTemperatureStateOperationServices() throws CouldNotPerformException;
}
