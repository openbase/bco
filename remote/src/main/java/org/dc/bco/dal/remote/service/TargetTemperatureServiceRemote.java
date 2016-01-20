/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.TargetTemperatureService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class TargetTemperatureServiceRemote extends AbstractServiceRemote<TargetTemperatureService> implements TargetTemperatureService {

    public TargetTemperatureServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    public void setTargetTemperature(Double value) throws CouldNotPerformException {
        for (TargetTemperatureService service : getServices()) {
            service.setTargetTemperature(value);
        }
    }

    @Override
    public Double getTargetTemperature() throws CouldNotPerformException {
        throw new CouldNotPerformException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
