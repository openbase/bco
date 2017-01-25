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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openbase.bco.dal.lib.layer.service.provider.PowerConsumptionStateProviderService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rsb.com.RSBRemote;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;
import rst.timing.TimestampType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface PowerConsumptionStateProviderServiceCollection extends PowerConsumptionStateProviderService {

    //TODO: is implemented in the service remotes but still used in the LocationController because else it would lead to too many unitRemots
    //remove when remote cashing is implemented
    /**
     * Returns an average current and voltage for the underlying provider and
     * the sum of their consumptions.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        try {
            // WORKAROUND HACK BEFORE LOCATION CONTROLLER IMPROVEMENT FEATURE IS READY
            double consumptionSum = 0;
            double averageCurrent = 0;
            double averageVoltage = 0;
            Collection<PowerConsumptionStateProviderService> powerConsumptionStateProviderServices = getPowerConsumptionStateProviderServices();
            int amount = powerConsumptionStateProviderServices.size();
            for (PowerConsumptionStateProviderService provider : getPowerConsumptionStateProviderServices()) {
                if (!((RSBRemote) provider).isDataAvailable()) {
                    amount--;
                    continue;
                }
                
                consumptionSum += provider.getPowerConsumptionState().getConsumption();
                averageCurrent += provider.getPowerConsumptionState().getCurrent();
                averageVoltage += provider.getPowerConsumptionState().getVoltage();
            }
            averageCurrent = averageCurrent / amount;
            averageVoltage = averageVoltage / amount;
            
            return PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(averageCurrent).setVoltage(averageVoltage).setTimestamp(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis())).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerConsumptionState", ex);
        }
    }

    public Collection<PowerConsumptionStateProviderService> getPowerConsumptionStateProviderServices() throws CouldNotPerformException;
}
