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

import org.openbase.bco.device.openhab.sitemap.SitemapBuilder;
import org.openbase.bco.device.openhab.sitemap.SitemapBuilder.SitemapIconType;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

public class GenericUnitSitemapElement extends AbstractUnitSitemapElement {

    final ServiceType serviceType;

    public GenericUnitSitemapElement(final String unitId) throws InstantiationException {
        super(unitId);
        this.serviceType = ServiceType.UNKNOWN;
    }

    public GenericUnitSitemapElement(final UnitConfig unitConfig) throws InstantiationException {
        super(unitConfig, false);
        this.serviceType = ServiceType.UNKNOWN;
    }

    public GenericUnitSitemapElement(final UnitConfig unitConfig, final ServiceType serviceType) throws InstantiationException {
        super(unitConfig, false);
        this.serviceType = serviceType;
    }

    public GenericUnitSitemapElement(final UnitConfig unitConfig, final boolean absolutLabel) throws InstantiationException {
        super(unitConfig, absolutLabel);
        this.serviceType = ServiceType.UNKNOWN;
    }

    public GenericUnitSitemapElement(final UnitConfig unitConfig, final ServiceType serviceType, final boolean absoluteLabel) throws InstantiationException {
        super(unitConfig, absoluteLabel);
        this.serviceType = serviceType;
    }


    @Override
    public void serialize(SitemapBuilder sitemap) throws CouldNotPerformException {

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
                sitemap.addTextElement(getItem(ServiceType.TAMPER_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.ERROR);
                break;
            case REED_CONTACT:
                sitemap.addTextElement(getItem(ServiceType.CONTACT_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.CONTACT);
                break;
            case MOTION_DETECTOR:
                sitemap.addTextElement(getItem(ServiceType.MOTION_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.MOTION);
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
            case DIMMER:
                sitemap.addDefaultElement(getItem(ServiceType.BRIGHTNESS_STATE_SERVICE), getLabel());
                break;
            case COLORABLE_LIGHT:
                sitemap.addColorpickerElement(getItem(ServiceType.COLOR_STATE_SERVICE), getLabel(), SitemapIconType.COLORWHEEL);
                break;
            case LIGHT_SENSOR:
                sitemap.addTextElement(getItem(ServiceType.ILLUMINANCE_STATE_SERVICE), getLabel() + " Helligkeit [%.1f Lux]", SitemapIconType.SUN);
                break;
            case BUTTON:
                sitemap.addTextElement(getItem(ServiceType.BUTTON_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.WALLSWITCH);
                break;
            case POWER_CONSUMPTION_SENSOR:
                sitemap.addTextElement(getItem(ServiceType.POWER_CONSUMPTION_STATE_SERVICE), getLabel() + " Vebrauch [%.1f Watt]", SitemapIconType.ENERGY);
                break;
            case USER:
                sitemap.addTextElement(getItem(ServiceType.PRESENCE_STATE_SERVICE), unitConfig.getUserConfig().getFirstName() + " " + unitConfig.getUserConfig().getLastName()  + "[%s]", SitemapIconType.MOTION);
                break;
            case ROLLER_SHUTTER:
                sitemap.addDefaultElement(getItem(ServiceType.BLIND_STATE_SERVICE), getLabel());
                break;
            case SWITCH:
                sitemap.addDefaultElement(getItem(ServiceType.SWITCH_STATE_SERVICE), getLabel());
                break;
            case SMOKE_DETECTOR:
                sitemap.addTextElement(getItem(ServiceType.SMOKE_ALARM_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.SIREN);
                sitemap.addTextElement(getItem(ServiceType.SMOKE_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.FIRE);
                break;
            case CONNECTION:
                switch (unitConfig.getConnectionConfig().getConnectionType()) {
                    case DOOR:
                        sitemap.addTextElement(getItem(ServiceType.DOOR_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.DOOR);
                        break;
                    case WINDOW:
                        sitemap.addTextElement(getItem(ServiceType.WINDOW_STATE_SERVICE), getLabel() + "[%s]", SitemapIconType.WINDOW);
                        break;
                    case PASSAGE:
                        sitemap.addDefaultElement(getItem(ServiceType.PASSAGE_STATE_SERVICE), getLabel() + "[%s]");
                        break;
                }
                break;
            case UNIT_GROUP:
                for (ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {

                    // filter non operation services
                    if(serviceConfig.getServiceDescription().getPattern() != ServicePattern.OPERATION) {
                        continue;
                    }

                    switch (serviceConfig.getServiceDescription().getServiceType()) {
                        case POWER_STATE_SERVICE:
                            sitemap.addSwitchElement(getItem(ServiceType.POWER_STATE_SERVICE), getLabel(), SitemapIconType.SWITCH);
                            break;
                        case COLOR_STATE_SERVICE:
                            sitemap.addColorpickerElement(getItem(ServiceType.COLOR_STATE_SERVICE), getLabel(), SitemapIconType.COLORWHEEL);
                            break;
                        case BRIGHTNESS_STATE_SERVICE:
                            sitemap.addDefaultElement(getItem(ServiceType.BRIGHTNESS_STATE_SERVICE), getLabel());
                            break;
                    }
                }
            case LOCATION:
                sitemap.addDefaultElement(getItem(ServiceType.POWER_STATE_SERVICE), getLabel());
                break;
            case GATEWAY:
            case DEVICE:
            case AUTHORIZATION_GROUP:
                break;
            case UNKNOWN:
            default:
                sitemap.addTextElement("", getLabel() + "[%s]");
        }
    }
}
