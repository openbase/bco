/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.TemperatureProvider;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface TemperatureStateProviderServiceCollection extends TemperatureProvider {

    /**
     * Returns the average temperature value for a collection of temperature
     * providers.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Double getTemperature() throws CouldNotPerformException {
        Double average = 0d;
        for (TemperatureProvider service : getTemperatureStateProviderServices()) {
            average += service.getTemperature();
        }
        average /= getTemperatureStateProviderServices().size();
        return average;
    }

    public Collection<TemperatureProvider> getTemperatureStateProviderServices();
}
