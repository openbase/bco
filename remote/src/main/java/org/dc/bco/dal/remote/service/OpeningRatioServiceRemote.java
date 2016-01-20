/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.OpeningRatioService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class OpeningRatioServiceRemote extends AbstractServiceRemote<OpeningRatioService> implements OpeningRatioService{

    public OpeningRatioServiceRemote(ServiceTemplateType.ServiceTemplate.ServiceType serviceType) {
        super(serviceType);
    }

    @Override
    public void setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        for(OpeningRatioService service : getServices()) {
            service.setOpeningRatio(openingRatio);
        }
    }

    @Override
    public Double getOpeningRatio() throws CouldNotPerformException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
