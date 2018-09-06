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
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;

public class RootLocationElement extends LocationElement {

    public RootLocationElement() throws InstantiationException {
        super();
        try {
            init(Registries.getUnitRegistry().getRootLocationConfig());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    @Override
    public void serialize(final SitemapBuilder sitemap) throws CouldNotPerformException {
        sitemap.openFrameContext("Allgemeine Informationen");
        sitemap.addTextElement("Date", "Datum");
        sitemap.addTextElement("Mails", "Mails");
        sitemap.addTextElement("SystemState", "System Status");
        sitemap.closeContext();

        super.serialize(sitemap);

        sitemap.openFrameContext("Alle Geräte");

        sitemap.openTextContext("Units", SitemapIconType.NONE);
        final Map<UnitType, List<UnitConfig>> unitTypeUnitConfigMap = new TreeMap<>();

        // load unit configs
        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
            if (!unitTypeUnitConfigMap.containsKey(unitConfig.getUnitType())) {
                unitTypeUnitConfigMap.put(unitConfig.getUnitType(), new ArrayList<>());
            }
            unitTypeUnitConfigMap.get(unitConfig.getUnitType()).add(unitConfig);
        }

        for (List<UnitConfig> unitConfigListValue : unitTypeUnitConfigMap.values()) {
            // sort by name
            Collections.sort(unitConfigListValue, Comparator.comparing(o -> label(o.getLabel())));
        }

        for (UnitType unitType : unitTypeUnitConfigMap.keySet()) {

            if (unitTypeUnitConfigMap.get(unitType).isEmpty()) {
                continue;
            }

            sitemap.openFrameContext(unitType.name());
            for (UnitConfig unitConfig : unitTypeUnitConfigMap.get(unitType)) {
                sitemap.append(new GenericUnitSitemapElement(unitConfig));
            }
            sitemap.closeContext();
        }
        sitemap.closeContext();

        sitemap.openTextContext("Services", SitemapIconType.NONE);
        final Map<ServiceType, List<UnitConfig>> serviceTypeUnitConfigMap = new TreeMap<>();

        // load unit configs
        for (UnitConfig unitConfig : Registries.getUnitRegistry().getUnitConfigs()) {
            for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                if (!serviceTypeUnitConfigMap.containsKey(serviceConfig.getServiceDescription().getServiceType())) {
                    serviceTypeUnitConfigMap.put(serviceConfig.getServiceDescription().getServiceType(), new ArrayList<>());
                }
                serviceTypeUnitConfigMap.get(serviceConfig.getServiceDescription().getServiceType()).add(unitConfig);
            }
        }

        for (List<UnitConfig> unitConfigListValue : serviceTypeUnitConfigMap.values()) {
            // sort by name
            Collections.sort(unitConfigListValue, Comparator.comparing(o -> label(o.getLabel())));
        }

        for (ServiceType serviceType : serviceTypeUnitConfigMap.keySet()) {

            if (serviceTypeUnitConfigMap.get(serviceType).isEmpty()) {
                continue;
            }

            sitemap.openFrameContext(serviceType.name());
            for (UnitConfig unitConfig : serviceTypeUnitConfigMap.get(serviceType)) {
                sitemap.append(new GenericUnitSitemapElement(unitConfig, serviceType));
            }
            sitemap.closeContext();
        }
        sitemap.closeContext();
        sitemap.closeContext();
    }
}
