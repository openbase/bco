package org.openbase.bco.dal.control.layer.unit.unitgroup;

/*-
 * #%L
 * BCO DAL Control
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroupController;
import org.openbase.bco.dal.lib.layer.unit.unitgroup.UnitGroupControllerFactory;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupControllerFactoryImpl implements UnitGroupControllerFactory {

    private static UnitGroupControllerFactoryImpl instance;

    public synchronized static UnitGroupControllerFactoryImpl getInstance() {
        if (instance == null) {
            instance = new UnitGroupControllerFactoryImpl();
        }
        return instance;
    }

    @Override
    public UnitGroupController newInstance(final UnitConfig config) throws InstantiationException, InterruptedException {
        UnitGroupControllerImpl unitGroupController;
        try {
            if (config == null) {
                throw new NotAvailableException("unitgroupconfig");
            }
            unitGroupController = new UnitGroupControllerImpl();
            unitGroupController.init(config);
            return unitGroupController;
        } catch (InstantiationException | NotAvailableException | InitializationException ex) {
            throw new InstantiationException(UnitGroupControllerFactoryImpl.class, config.getId(), ex);
        }
    }
}
