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
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProviderService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface PowerConsumptionStateProviderServiceCollection extends PowerConsumptionProviderService {

    /**
     * Returns an average current and voltage for the underlying provider and
     * the sum of their consumptions.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default public PowerConsumptionState getPowerConsumption() throws NotAvailableException {
        try {
            double consumptionSum = 0;
            double averageCurrent = 0;
            double averageVoltage = 0;
            for (PowerConsumptionProviderService provider : getPowerConsumptionStateProviderServices()) {
                consumptionSum += provider.getPowerConsumption().getConsumption();
                averageCurrent += provider.getPowerConsumption().getCurrent();
                averageVoltage += provider.getPowerConsumption().getVoltage();
            }
            averageCurrent = averageCurrent / getPowerConsumptionStateProviderServices().size();
            averageVoltage = averageVoltage / getPowerConsumptionStateProviderServices().size();
            return PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(averageCurrent).setVoltage(averageVoltage).build();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("PowerConsumptionState", ex);
        }
    }

    public Collection<PowerConsumptionProviderService> getPowerConsumptionStateProviderServices() throws CouldNotPerformException;
}
