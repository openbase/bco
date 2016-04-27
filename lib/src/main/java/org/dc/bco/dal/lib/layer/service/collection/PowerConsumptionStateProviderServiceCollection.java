/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface PowerConsumptionStateProviderServiceCollection extends PowerConsumptionProvider {

    /**
     * Returns an average current and voltage for the underlying provider and
     * the sum of their consumptions.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public PowerConsumptionState getPowerConsumption() throws CouldNotPerformException {
        double consumptionSum = 0;
        double averageCurrent = 0;
        double averageVoltage = 0;
        for (PowerConsumptionProvider provider : getPowerConsumptionStateProviderServices()) {
            consumptionSum += provider.getPowerConsumption().getConsumption();
            averageCurrent += provider.getPowerConsumption().getCurrent();
            averageVoltage += provider.getPowerConsumption().getVoltage();
        }
        averageCurrent = averageCurrent / getPowerConsumptionStateProviderServices().size();
        averageVoltage = averageVoltage / getPowerConsumptionStateProviderServices().size();
        return PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(averageCurrent).setVoltage(averageVoltage).build();
    }

    public Collection<PowerConsumptionProvider> getPowerConsumptionStateProviderServices();
}
