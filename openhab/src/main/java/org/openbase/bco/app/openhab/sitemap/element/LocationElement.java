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

import org.openbase.bco.app.openhab.sitemap.SitemapBuilder;
import org.openbase.bco.app.openhab.sitemap.SitemapBuilder.SitemapIconType;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.List;

public class LocationElement extends AbstractUnitSitemapElement {

    public LocationElement(final String unitId) throws InstantiationException {
        super(unitId);
    }

    @Override
    public void serialize(SitemapBuilder sitemap) throws CouldNotPerformException {

        sitemap.openFrameContext("Übersicht");
        sitemap.addTextElement(getItem(ServiceType.TEMPERATURE_STATE_SERVICE), "Raumtemperatur [%.1f °C]");
        sitemap.addTextElement(getItem(ServiceType.PRESENCE_STATE_SERVICE), "Anwesenheit");
        sitemap.addTextElement(getItem(ServiceType.ILLUMINANCE_STATE_SERVICE), "Helligkeit [%.1f Lux]");
        sitemap.closeContext();

        sitemap.openFrameContext("Steuerung");
        sitemap.addColorpickerElement(getItem(ServiceType.POWER_STATE_SERVICE), "Raumfarbe", SitemapIconType.COLORWHEEL);
        sitemap.addSwitchElement(getItem(ServiceType.POWER_STATE_SERVICE), "Geräte", SitemapIconType.SWITCH);
        sitemap.addSwitchElement(getItem(ServiceType.STANDBY_STATE_SERVICE), "Standby", SitemapIconType.SWITCH);
        sitemap.addSliderElement(getItem(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE), "Wunschtemperatur [%.1f °C]", SitemapIconType.HEATING);
        sitemap.closeContext();

//        sitemap.openFrameContext("Aktivitäten");
//        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.AC, unitConfig.getId())) {
//            sitemap.append(new GenericUnitSitemapElement(unitConfig));
//        }
//        sitemap.closeContext();


        List<UnitConfig> unitConfigList;

        // add scenes
        unitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.SCENE, unitConfig.getId());
        if (!unitConfigList.isEmpty()) {
            sitemap.openFrameContext("Scenen");
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.SCENE, unitConfig.getId())) {
                sitemap.append(new GenericUnitSitemapElement(unitConfig));
            }
            sitemap.closeContext();
        }

        // add apps
        unitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.APP, unitConfig.getId());
        if (!unitConfigList.isEmpty()) {
            sitemap.openFrameContext("Apps");
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.APP, unitConfig.getId())) {
                sitemap.append(new GenericUnitSitemapElement(unitConfig));
            }
            sitemap.closeContext();
        }

        // add sublocations
        if(!unitConfig.getLocationConfig().getChildIdList().isEmpty()) {
            sitemap.openFrameContext("Unterbereiche");
            for (String childId : unitConfig.getLocationConfig().getChildIdList()) {
                sitemap.openTextContext(getLabel(), SitemapIconType.CORRIDOR);
                sitemap.append(new LocationElement(childId));
                sitemap.closeContext();
            }
            sitemap.closeContext();
        }

        unitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocation(UnitType.AGENT, unitConfig.getId());
        if (!unitConfigList.isEmpty() || !unitConfig.getLocationConfig().getUnitIdList().isEmpty()) {
            sitemap.openFrameContext("Sonstiges");
            // add agents
            if (!unitConfigList.isEmpty()) {
                sitemap.openTextContext("Verhaltensweisen", SitemapIconType.NONE);
                for (UnitConfig unitConfig : unitConfigList) {
                    sitemap.append(new GenericUnitSitemapElement(unitConfig));
                }
                sitemap.closeContext();
            }

            // add all other units
            if (!unitConfig.getLocationConfig().getUnitIdList().isEmpty()) {
                sitemap.openTextContext("Geräte Übersicht", SitemapIconType.NONE);
                for (String unitId : unitConfig.getLocationConfig().getUnitIdList()) {
                    sitemap.append(new GenericUnitSitemapElement(unitId));
                }
                sitemap.closeContext();
            }
            sitemap.closeContext();
        }
    }
}
