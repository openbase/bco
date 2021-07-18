package org.openbase.bco.registry.unit.core.consistency.authorizationgroup;

/*
 * #%L
 * BCO Registry Unit Core
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

import org.openbase.bco.registry.unit.core.consistency.DefaultUnitLabelConsistencyHandler;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

/**
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class AuthorizationGroupConfigLabelConsistencyHandler extends DefaultUnitLabelConsistencyHandler {

    /**
     * Label for authorization groups have to be globally unique.
     * Therefore just return the label.
     *
     * @param label       the label for which the key is generated
     * @param languageKey the language key of the label.
     * @param unitConfig  the unit having the label
     *
     * @return the label
     */
    @Override
    protected String generateKey(final String label, final String languageKey, final UnitConfig unitConfig) {
        return label + "_" + languageKey;
    }
}
