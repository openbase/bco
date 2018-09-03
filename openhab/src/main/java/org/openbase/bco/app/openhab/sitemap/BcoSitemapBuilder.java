package org.openbase.bco.app.openhab.sitemap;

/*-
 * #%L
 * BCO Openhab App
 * %%
 * Copyright (C) 2018 openbase.org
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

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rst.configuration.LabelType.Label;

public class BcoSitemapBuilder extends AbstractSitemapBuilder {

    public static final String FILENAME = "bco";

    public BcoSitemapBuilder() throws InstantiationException {
        super(FILENAME, getRootLocationLabel());
    }

    private static Label getRootLocationLabel() throws InstantiationException {
        try {
            return Registries.getUnitRegistry().getRootLocationConfig().getLabel();
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(BcoSitemapBuilder.class, ex);
        }
    }
}
