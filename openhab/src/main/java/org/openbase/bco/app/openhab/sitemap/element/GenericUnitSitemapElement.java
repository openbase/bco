package org.openbase.bco.app.openhab.sitemap.element;

/*-
 * #%L
 * BCO Openhab Device Manager
 * %%
 * Copyright (C) 2015 - 2018 openbase.org
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
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class GenericUnitSitemapElement extends AbstractUnitSitemapElement {

    final ServiceType serviceType;

    public GenericUnitSitemapElement(final String unitId) throws InstantiationException {
        super(unitId);
        this.serviceType = ServiceType.UNKNOWN;
    }

    public GenericUnitSitemapElement(final UnitConfig unitConfig) throws InstantiationException {
        super(unitConfig);
        this.serviceType = ServiceType.UNKNOWN;
    }

    public GenericUnitSitemapElement(final UnitConfig unitConfig, final ServiceType serviceType) throws InstantiationException {
        super(unitConfig);
        this.serviceType = serviceType;
    }


    @Override
    public void serialize(SitemapBuilder sitemap) throws CouldNotPerformException {


        if (serviceType != ServiceType.UNKNOWN) {
            sitemap.addDefaultElement(getItem(serviceType), getLabel());
            return;
        }

        switch (unitConfig.getUnitType()) {
            case SCENE:
            case APP:
            case AGENT:
                sitemap.addSwitchElement(getItem(ServiceType.ACTIVATION_STATE_SERVICE), getLabel(), SitemapIconType.SWITCH);
                break;
            case POWER_SWITCH:
                sitemap.addSwitchElement(getItem(ServiceType.POWER_STATE_SERVICE), getLabel(), SitemapIconType.SWITCH);
                break;
            case TAMPER_DETECTOR:
            case REED_CONTACT:
                sitemap.addDefaultElement(getItem(ServiceType.POWER_STATE_SERVICE), getLabel());
                break;
            case MOTION_DETECTOR:
                sitemap.addDefaultElement(getItem(ServiceType.MOTION_STATE_SERVICE), getLabel());
                break;
            case TEMPERATURE_SENSOR:
                sitemap.addTextElement(getItem(ServiceType.TEMPERATURE_STATE_SERVICE), getLabel() + " Temperatur [%.1f °C]");
                break;
            case TEMPERATURE_CONTROLLER:
                sitemap.addDefaultElement(getItem(ServiceType.TARGET_TEMPERATURE_STATE_SERVICE), getLabel());
                break;
            case BATTERY:
                sitemap.addDefaultElement(getItem(ServiceType.BATTERY_STATE_SERVICE), getLabel());
                break;
            case LIGHT:
                sitemap.addSwitchElement(getItem(ServiceType.POWER_STATE_SERVICE), getLabel(), SitemapIconType.LIGHT);
                break;
            case DIMMABLE_LIGHT:
                sitemap.addDefaultElement(getItem(ServiceType.BRIGHTNESS_STATE_SERVICE), getLabel());
                break;
            case COLORABLE_LIGHT:
                sitemap.addColorpickerElement(getItem(ServiceType.COLOR_STATE_SERVICE), getLabel(), SitemapIconType.COLORWHEEL);
                break;
            case LIGHT_SENSOR:
                sitemap.addTextElement(getItem(ServiceType.ILLUMINANCE_STATE_SERVICE), getLabel() + " Helligkeit [%.1f Lux]");
                break;
            case BUTTON:
                sitemap.addDefaultElement(getItem(ServiceType.BUTTON_STATE_SERVICE), getLabel());
                break;
            case POWER_CONSUMPTION_SENSOR:
                sitemap.addTextElement(getItem(ServiceType.ILLUMINANCE_STATE_SERVICE), getLabel() + " Vebrauch [%.1f Watt]");
                break;
            case UNKNOWN:
            default:
                sitemap.addTextElement("", getLabel());
        }
    }
}
