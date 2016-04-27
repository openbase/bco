/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.ColorService;
import org.dc.jul.exception.CouldNotPerformException;
import rst.vision.HSVColorType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface ColorStateOperationServiceCollection extends ColorService {

    @Override
    default public void setColor(final HSVColorType.HSVColor color) throws CouldNotPerformException {
        for (ColorService service : getColorStateOperationServices()) {
            service.setColor(color);
        }
    }

    @Override
    default public HSVColorType.HSVColor getColor() throws CouldNotPerformException {
        for (ColorService service : getColorStateOperationServices()) {
            return service.getColor();
        }
        throw new CouldNotPerformException("Not supported yet.");
    }

    public Collection<ColorService> getColorStateOperationServices();
}
