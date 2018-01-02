package unitgroup;

/*-
 * #%L
 * BCO Manager Location Core
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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

import org.openbase.bco.manager.location.lib.unitgroup.UnitGroupController;
import org.openbase.bco.manager.location.lib.unitgroup.UnitGroupFactory;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:pLeminoq@openbase.org">Tamino Huxohl</a>
 */
public class UnitGroupFactoryImpl implements UnitGroupFactory {

    private static UnitGroupFactoryImpl instance;

    public synchronized static UnitGroupFactoryImpl getInstance() {
        if (instance == null) {
            instance = new UnitGroupFactoryImpl();
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
            throw new InstantiationException(UnitGroupFactoryImpl.class, config.getId(), ex);
        }
    }
}
