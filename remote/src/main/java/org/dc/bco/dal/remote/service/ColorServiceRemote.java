/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;
import rst.vision.HSVColorType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ColorServiceRemote extends AbstractServiceRemote<ColorService> implements ColorService {

    public ColorServiceRemote() {
        super(ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_SERVICE);
    }

    @Override
    public void setColor(final HSVColorType.HSVColor color) throws CouldNotPerformException {
        for (ColorService service : getServices()) {
            service.setColor(color);
        }
    }

    @Override
    public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
        throw new CouldNotPerformException("Not supported yet.");
    }
}
