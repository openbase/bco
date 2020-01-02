package org.openbase.bco.registry.activity.core.dbconvert;

/*-
 * #%L
 * BCO Registry Activity Core
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.registry.lib.dbconvert.LabelDBConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 * Converter for activity configs which updates them to the new label structure.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class ActivityConfig_1_To_2_DBConverter extends LabelDBConverter {

    public ActivityConfig_1_To_2_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }
}
