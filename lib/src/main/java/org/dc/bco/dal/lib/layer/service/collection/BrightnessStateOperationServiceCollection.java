/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.jul.exception.CouldNotPerformException;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface BrightnessStateOperationServiceCollection extends BrightnessService {

    @Override
    default public void setBrightness(Double brightness) throws CouldNotPerformException {
        for (BrightnessService service : getBrightnessStateOperationServices()) {
            service.setBrightness(brightness);
        }
    }

    /**
     * Returns the average brightness value for a collection of brightness
     * services.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public Double getBrightness() throws CouldNotPerformException {
        Double average = 0d;
        for (BrightnessService service : getBrightnessStateOperationServices()) {
            average += service.getBrightness();
        }
        average /= getBrightnessStateOperationServices().size();
        return average;
    }

    public Collection<BrightnessService> getBrightnessStateOperationServices();
}
