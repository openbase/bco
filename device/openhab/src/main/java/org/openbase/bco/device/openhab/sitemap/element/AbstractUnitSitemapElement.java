package org.openbase.bco.device.openhab.sitemap.element;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2021 openbase.org
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

import org.openbase.bco.device.openhab.registry.synchronizer.OpenHABItemProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.iface.Initializable;
import org.openbase.jul.iface.provider.LabelProvider;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public abstract class AbstractUnitSitemapElement extends AbstractSitemapElement implements Initializable<UnitConfig>, LabelProvider {

    protected final boolean absoluteLabel;
    protected UnitConfig unitConfig;
    protected UnitConfig parentUnitConfig;

    public AbstractUnitSitemapElement() throws InstantiationException {
        this.absoluteLabel = false;
    }

    public AbstractUnitSitemapElement(final String unitId) throws InstantiationException {
        try {
            this.absoluteLabel = false;
            init(Registries.getUnitRegistry().getUnitConfigById(unitId));
            this.parentUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    public AbstractUnitSitemapElement(final UnitConfig unitConfig, final boolean absoluteLabel) throws InstantiationException {
        this.absoluteLabel = absoluteLabel;
        init(unitConfig);
        try {
            this.parentUnitConfig = Registries.getUnitRegistry().getUnitConfigById(unitConfig.getPlacementConfig().getLocationId());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }


    @Override
    public void init(UnitConfig unitConfig) {
        this.unitConfig = unitConfig;
    }

    @Override
    public String getLabel() {
        return LabelProcessor.getBestMatch(unitConfig.getLabel(), "?") + (absoluteLabel && parentUnitConfig != null ? " @ " +
                LabelProcessor.getBestMatch(parentUnitConfig.getLabel(), "?") : "");
    }

    protected String getItem(final ServiceType serviceType) {
        return OpenHABItemProcessor.generateItemName(unitConfig, serviceType);
    }
}
