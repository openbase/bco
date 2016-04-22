/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.provider.PowerConsumptionProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.state.PowerConsumptionStateType.PowerConsumptionState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PowerConsumptionProviderRemote extends AbstractServiceRemote<PowerConsumptionProvider> implements PowerConsumptionProvider {

    public PowerConsumptionProviderRemote() {
        super(ServiceType.POWER_CONSUMPTION_PROVIDER);
    }

    /**
     * Returns an average current and voltage for the underlying provider and
     * the sum of their consumptions.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public PowerConsumptionState getPowerConsumption() throws CouldNotPerformException {
        double consumptionSum = 0;
        double averageCurrent = 0;
        double averageVoltage = 0;
        for (PowerConsumptionProvider provider : getServices()) {
            consumptionSum += provider.getPowerConsumption().getConsumption();
            averageCurrent += provider.getPowerConsumption().getCurrent();
            averageVoltage += provider.getPowerConsumption().getVoltage();
        }
        averageCurrent = averageCurrent / getServices().size();
        averageVoltage = averageVoltage / getServices().size();
        return PowerConsumptionState.newBuilder().setConsumption(consumptionSum).setCurrent(averageCurrent).setVoltage(averageVoltage).build();
    }
}
