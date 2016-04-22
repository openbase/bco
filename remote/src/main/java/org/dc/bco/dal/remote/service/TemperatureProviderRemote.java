/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.provider.TemperatureProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class TemperatureProviderRemote extends AbstractServiceRemote<TemperatureProvider> implements TemperatureProvider {

    public TemperatureProviderRemote() {
        super(ServiceType.TEMPERATURE_PROVIDER);
    }

    /**
     * Returns the average temperature value for a collection of temperature
     * providers.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    public Double getTemperature() throws CouldNotPerformException {
        Double average = 0d;
        for (TemperatureProvider service : getServices()) {
            average += service.getTemperature();
        }
        average /= getServices().size();
        return average;
    }
}
