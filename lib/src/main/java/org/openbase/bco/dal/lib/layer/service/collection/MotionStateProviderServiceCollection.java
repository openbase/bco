package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import org.openbase.bco.dal.lib.layer.service.provider.MotionStateProviderService;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.state.MotionStateType.MotionState;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface MotionStateProviderServiceCollection extends MotionStateProviderService {

    /**
     * Computes the motion state as motion if at least one underlying services replies with motion and else no motion.
     * Additionally the last motion timestamp is set as the latest of the underlying services.
     *
     * @return
     * @throws NotAvailableException
     */
    @Override
    default MotionState getMotionState() throws NotAvailableException {
        return MotionStateProviderService.super.getMotionState();
    }

    /**
     * Computes the motion state as motion if at least one underlying services with given unitType replies with motion and else no motion.
     * Additionally the last motion timestamp is set as the latest of the underlying services.
     *
     * @param unitType
     * @return
     * @throws NotAvailableException
     */
    MotionState getMotionState(final UnitType unitType) throws NotAvailableException;
}
