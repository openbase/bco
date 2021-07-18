package org.openbase.bco.dal.remote.printer;

/*-
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public class IdResolver {

    public static final boolean SIMPLE = true;

    public static String getId(final String unitId) throws NotAvailableException {
        return getId(Registries.getUnitRegistry().getUnitConfigById(unitId));
    }

    public static String getId(final Unit<?> unit) throws NotAvailableException {
        return getId(unit.getConfig());
    }

    public static String getId(final UnitConfig unitConfig) {
        if (SIMPLE) {
            return unitConfig.getAlias(0)+"["+StringProcessor.transformToIdString(LabelProcessor.getBestMatch(unitConfig.getLabel(), ""))+"]";
        } else {
            return unitConfig.getId();
        }
    }
}
