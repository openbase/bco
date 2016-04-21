/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.unit;

import java.lang.reflect.Method;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.service.ServiceTemplateType;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public interface UnitController extends Unit {

    /**
     * Returns the service state update method for the given service type.
     * @param serviceType
     * @param serviceArgumentClass
     * @return
     * @throws CouldNotPerformException
     */
    public Method getUpdateMethod(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType, Class serviceArgumentClass) throws CouldNotPerformException;

    /**
     * Applies the given service state update for this unit.
     * @param serviceType
     * @param serviceArgument
     * @throws CouldNotPerformException
     */
    public void applyUpdate(final ServiceTemplateType.ServiceTemplate.ServiceType serviceType, Object serviceArgument) throws CouldNotPerformException;
}
