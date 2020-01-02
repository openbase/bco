package org.openbase.bco.device.openhab.sitemap.element;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2020 openbase.org
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

import org.openbase.bco.device.openhab.sitemap.SitemapBuilder;
import org.openbase.bco.device.openhab.sitemap.SitemapBuilder.SitemapIconType;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;
import java.util.Map.Entry;

public class LocationElement extends AbstractUnitSitemapElement {

    public LocationElement() throws InstantiationException {
        super();
    }

    public LocationElement(final String unitId) throws InstantiationException {
        super(unitId);
    }

    @Override
    public void serialize(SitemapBuilder sitemap) throws CouldNotPerformException {

        // add sublocations
        if (!unitConfig.getLocationConfig().getChildIdList().isEmpty()) {
            sitemap.openFrameContext("Bereiche");
            final Map<String, UnitConfig> labelSortedUnitConfigMap = new TreeMap<>();
            for (String childId : unitConfig.getLocationConfig().getChildIdList()) {
                final UnitConfig locationUnitConfig = Registries.getUnitRegistry().getUnitConfigById(childId);
                labelSortedUnitConfigMap.put(LabelProcessor.getBestMatch(locationUnitConfig.getLabel(),"?"), locationUnitConfig);
            }
            for (Entry<String, UnitConfig> labelUnitConfigEntry : labelSortedUnitConfigMap.entrySet()) {
                sitemap.openTextContext(labelUnitConfigEntry.getKey(), SitemapIconType.CORRIDOR);
                sitemap.append(new LocationElement(labelUnitConfigEntry.getValue().getId()));
                sitemap.closeContext();
            }
            sitemap.closeContext();
        }

        sitemap.openFrameContext("Übersicht");
        sitemap.addTextElement(getItem(ServiceType.TEMPERATURE_STATE_SERVICE), "Raumtemperatur [%.1f °C]", SitemapIconType.TEMPERATURE);
        sitemap.addTextElement(getItem(ServiceType.PRESENCE_STATE_SERVICE), "Anwesenheit [%s]", SitemapIconType.MOTION);
        sitemap.addTextElement(getItem(ServiceType.ILLUMINANCE_STATE_SERVICE), "Helligkeit [%.1f Lux]", SitemapIconType.SUN);
        sitemap.closeContext();

        sitemap.openFrameContext("Steuerung");
        sitemap.addColorpickerElement(getItem(ServiceType.COLOR_STATE_SERVICE), "Raumfarbe", SitemapIconType.COLORWHEEL);
        sitemap.addSwitchElement(getItem(ServiceType.POWER_STATE_SERVICE), "Geräte", SitemapIconType.SWITCH);
        sitemap.addSwitchElement(getItem(ServiceType.STANDBY_STATE_SERVICE), "Standby", SitemapIconType.SWITCH);
        sitemap.addSliderElement(getItem(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE), "Wunschtemperatur [%.1f °C]", SitemapIconType.HEATING);
        sitemap.closeContext();

//        sitemap.openFrameContext("Aktivitäten");
//        for (UnitConfig unitConfig : Registries.getActivityRegistry().getActivityConfigByLocation(UnitType., unitConfig.getId())) {
//            sitemap.append(new GenericUnitSitemapElement(unitConfig));
//        }
//        sitemap.closeContext();


        List<UnitConfig> unitConfigList;

        // add scenes
        unitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitTypeRecursive(unitConfig.getId(), UnitType.SCENE, false);
        if (!unitConfigList.isEmpty()) {
            sitemap.openFrameContext("Scenen");
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(unitConfig.getId(), UnitType.SCENE)) {
                sitemap.append(new GenericUnitSitemapElement(unitConfig));
            }
            sitemap.closeContext();
        }

        // add apps
        unitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitTypeRecursive(unitConfig.getId(), UnitType.APP, false);
        if (!unitConfigList.isEmpty()) {
            sitemap.openFrameContext("Apps");
            for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitType(unitConfig.getId(), UnitType.APP)) {
                sitemap.append(new GenericUnitSitemapElement(unitConfig));
            }
            sitemap.closeContext();
        }

        unitConfigList = Registries.getUnitRegistry().getUnitConfigsByLocationIdAndUnitTypeRecursive(unitConfig.getId(), UnitType.AGENT,false);
        if (!unitConfigList.isEmpty() || !unitConfig.getLocationConfig().getUnitIdList().isEmpty()) {
            sitemap.openFrameContext("Sonstiges");
            // add agents
            if (!unitConfigList.isEmpty()) {
                sitemap.openTextContext("Verhaltensweisen", SitemapIconType.CHART);
                for (UnitConfig unitConfig : unitConfigList) {
                    sitemap.append(new GenericUnitSitemapElement(unitConfig));
                }
                sitemap.closeContext();
            }

            // add all other units
            if (!unitConfig.getLocationConfig().getUnitIdList().isEmpty()) {
                sitemap.openTextContext("Geräte Übersicht", SitemapIconType.FLOW);
                final Map<UnitType, List<UnitConfig>> unitTypeUnitConfigMap = new TreeMap<>();


                // load unit configs
                for (final UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigsByLocationIdRecursive(unitConfig.getId(), false)) {

                    // filter devices
                    if (unitConfig.getUnitType() == UnitType.DEVICE) {
                        continue;
                    }

                    // filter user
                    if (unitConfig.getUnitType() == UnitType.USER) {
                        continue;
                    }

                    // filter auth groups
                    if (unitConfig.getUnitType() == UnitType.AUTHORIZATION_GROUP) {
                        continue;
                    }

                    // filter buttons
                    if (unitConfig.getUnitType() == UnitType.BUTTON) {
                        continue;
                    }

                    if (!unitTypeUnitConfigMap.containsKey(unitConfig.getUnitType())) {
                        unitTypeUnitConfigMap.put(unitConfig.getUnitType(), new ArrayList<>());
                    }
                    unitTypeUnitConfigMap.get(unitConfig.getUnitType()).add(unitConfig);
                }

                for (List<UnitConfig> unitConfigListValue : unitTypeUnitConfigMap.values()) {
                    // sort by name
                    Collections.sort(unitConfigListValue, Comparator.comparing(o -> LabelProcessor.getBestMatch(o.getLabel(), "?")));
                }

                for (UnitType unitType : unitTypeUnitConfigMap.keySet()) {

                    if (unitTypeUnitConfigMap.get(unitType).isEmpty()) {
                        continue;
                    }

                    sitemap.openFrameContext(StringProcessor.formatHumanReadable(StringProcessor.transformUpperCaseToPascalCase(unitType.name())));
                    for (UnitConfig unitConfig : unitTypeUnitConfigMap.get(unitType)) {
                        sitemap.append(new GenericUnitSitemapElement(unitConfig));
                    }
                    sitemap.closeContext();
                }
                sitemap.closeContext();
            }
            sitemap.closeContext();
        }
    }
}
