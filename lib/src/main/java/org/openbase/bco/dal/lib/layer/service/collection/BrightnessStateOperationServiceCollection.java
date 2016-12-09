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
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import rst.domotic.state.BrightnessStateType.BrightnessState;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface BrightnessStateOperationServiceCollection extends BrightnessStateOperationService {

    @Override
    default public Future<Void> setBrightnessState(final BrightnessState brightnessState) throws CouldNotPerformException {
        return GlobalCachedExecutorService.allOf((BrightnessStateOperationService input) -> input.setBrightnessState(brightnessState), getBrightnessStateOperationServices());
    }

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns the average brightness value for a collection of brightness
     * services.
     *
     * @return
     * @throws org.openbase.jul.exception.NotAvailableException
     */
    @Override
    default public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            Double average = 0d;
            for (BrightnessStateOperationService service : getBrightnessStateOperationServices()) {
                average += service.getBrightnessState().getBrightness();
            }
            average /= getBrightnessStateOperationServices().size();
            return BrightnessState.newBuilder().setBrightness(average).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("Brightness", ex);
        }
    }

    public Collection<BrightnessStateOperationService> getBrightnessStateOperationServices() throws CouldNotPerformException;
}
