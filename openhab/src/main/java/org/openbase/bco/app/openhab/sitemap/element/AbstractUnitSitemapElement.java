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

import org.openbase.bco.app.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.LabelProcessor;
import org.openbase.jul.iface.Initializable;
import org.openbase.jul.iface.provider.LabelProvider;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public abstract class AbstractUnitSitemapElement extends AbstractSitemapElement implements Initializable<UnitConfig>, LabelProvider {

    protected UnitConfig unitConfig;

    public AbstractUnitSitemapElement() throws InstantiationException {
    }

    public AbstractUnitSitemapElement(final String unitId) throws InstantiationException {
        try {
            init(Registries.getUnitRegistry().getUnitConfigById(unitId));
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public AbstractUnitSitemapElement(final UnitConfig unitConfig) throws InstantiationException {
        init(unitConfig);
    }


    @Override
    public void init(UnitConfig unitConfig) {
        this.unitConfig = unitConfig;
    }

    @Override
    public String getLabel() {
        try {
            return LabelProcessor.getBestMatch(unitConfig.getLabel());
        } catch (NotAvailableException ex) {
            return "?";
        }
    }

    protected String getItem(final ServiceType serviceType) {
        return OpenHABItemProcessor.generateItemName(unitConfig, serviceType);
    }
}
