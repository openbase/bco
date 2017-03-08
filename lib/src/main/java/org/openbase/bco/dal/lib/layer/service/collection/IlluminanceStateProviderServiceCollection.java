/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openbase.bco.dal.lib.layer.service.collection;

import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.state.IlluminanceStateType.IlluminanceState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import org.openbase.bco.dal.lib.layer.service.provider.IlluminanceStateProviderService;

/**
 *
 * @author pleminoq
 */
public interface IlluminanceStateProviderServiceCollection extends IlluminanceStateProviderService {

    /**
     * Compute the average illuminance measured by the underlying services.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    public IlluminanceState getIlluminanceState() throws NotAvailableException;

    /**
     * Compute the average illuminance measured by the underlying services with given unitType.
     *
     * @param unitType
     * @return
     * @throws NotAvailableException
     */
    public IlluminanceState getIlluminanceState(final UnitType unitType) throws NotAvailableException;

}
