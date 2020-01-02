package org.openbase.bco.registry.unit.core.dbconvert;

/*-
 * #%L
 * BCO Registry Unit Core
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

import org.openbase.bco.registry.lib.dbconvert.DescriptionBCO2DBConverter;
import org.openbase.jul.storage.registry.version.DBVersionControl;

/**
 * Converter which updates scenes to the new label structure.
 * Additionally responsible actions and timestamps are cleared from serialized service attributes.
 * This is required because the action description changed and thus these service attributes can no longer
 * be de-serialized. These values should not have been serialized to begin with.
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class SceneConfig_5_To_6_DBConverter  extends DescriptionBCO2DBConverter {

    public SceneConfig_5_To_6_DBConverter(DBVersionControl versionControl) {
        super(versionControl);
    }
}
