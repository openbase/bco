package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
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
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalExecutionService;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface BrightnessStateOperationServiceCollection extends BrightnessOperationService {

    @Override
    default public Future<Void> setBrightness(final Double brightness) throws CouldNotPerformException {
        return GlobalExecutionService.allOf((BrightnessOperationService input) -> input.setBrightness(brightness), getBrightnessStateOperationServices());
    }

    /**
     * Returns the average brightness value for a collection of brightness
     * services.
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    default public Double getBrightness() throws NotAvailableException {
        try {
            Double average = 0d;
            for (BrightnessOperationService service : getBrightnessStateOperationServices()) {
                average += service.getBrightness();
            }
            average /= getBrightnessStateOperationServices().size();
            return average;
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Brightness", ex);
        }
    }

    public Collection<BrightnessOperationService> getBrightnessStateOperationServices() throws CouldNotPerformException;
}
