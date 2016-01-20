/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.BrightnessService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class BrightnessServiceRemote extends AbstractServiceRemote<BrightnessService> implements BrightnessService {

    public BrightnessServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_SERVICE);
    }

    @Override
    public void setBrightness(Double brightness) throws CouldNotPerformException {
        for (BrightnessService service : getServices()) {
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
    public Double getBrightness() throws CouldNotPerformException {
        Double average = 0d;
        for (BrightnessService service : getServices()) {
            average += service.getBrightness();
        }
        average /= getServices().size();
        return average;
    }
}
