package org.openbase.bco.dal.control.layer.unit;

/*
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

import org.openbase.bco.dal.lib.layer.unit.HostUnitController;
import org.openbase.bco.dal.lib.layer.unit.RollerShutter;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.unit.dal.RollerShutterDataType.RollerShutterData;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class RollerShutterController extends AbstractDALUnitController<RollerShutterData, RollerShutterData.Builder> implements RollerShutter {

    public RollerShutterController(final HostUnitController hostUnitController, final RollerShutterData.Builder builder) throws InstantiationException {
        super(hostUnitController, builder);
    }
}
