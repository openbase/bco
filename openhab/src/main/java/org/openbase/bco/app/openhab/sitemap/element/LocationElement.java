package org.openbase.bco.app.openhab.sitemap.element;

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
import org.openbase.jul.extension.rst.processing.LabelProcessor;

public class LocationElement extends AbstractUnitSitemapElement {

    public LocationElement(final String unitId, AbstractSitemapElement parentElement)throws InstantiationException {
        super(unitId, parentElement);
    }

    @Override
    protected String serialize(String sitemap) throws CouldNotPerformException {
        sitemap += prefix + "Text item=\"" +unitConfig.getAlias(0)+"\" label=\""+LabelProcessor.getBestMatch(unitConfig.getLabel())+"\" icon=\"video\" {" + System.lineSeparator();

        for (String childUnit : unitConfig.getLocationConfig().getChildIdList()) {
            sitemap = new GenericUnitSitemapElement(childUnit,this).appendElement(sitemap);
        }



       // sitemap += prefix + tab() + "Switch item="+unitConfig.getUnitType().name()+" icon=\"Light\""  + System.lineSeparator();
        sitemap += prefix + tab() + "Switch item=\"" +unitConfig.getAlias(0)+"\" label=\"Power\" icon=\"light\""  + System.lineSeparator();
        for (String childId : unitConfig.getLocationConfig().getChildIdList()) {
            sitemap = new LocationElement(childId, this).appendElement(sitemap);
        }
        sitemap += prefix + "}" + System.lineSeparator();
        return sitemap;
    }
}
