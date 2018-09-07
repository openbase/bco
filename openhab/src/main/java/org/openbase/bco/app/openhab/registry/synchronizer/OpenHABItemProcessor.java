package org.openbase.bco.app.openhab.registry.synchronizer;

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

import org.openbase.bco.registry.unit.core.consistency.UnitAliasGenerationConsistencyHandler;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class OpenHABItemProcessor {

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";

    //TODO: maybe these values are somewhere available in eclipse smart home
    public static final String OPENHAB_COLOR_TYPE = "Color"; // Color information (RGB);	OnOff, IncreaseDecrease, Percent, HSB
    public static final String OPENHAB_CONTACT_TYPE = "Contact"; // Status of contacts, e.g. door/window contacts;	OpenClose
    public static final String OPENHAB_DATE_TIME_TYPE = "DateTime"; // Stores date and time;
    public static final String OPENHAB_DIMMER_TYPE = "Dimmer"; // Percentage value for dimmers;	OnOff, IncreaseDecrease, Percent
    public static final String OPENHAB_GROUP_TYPE = "Group"; // Item to nest other items / collect them in groups;
    public static final String OPENHAB_IMAGER_TYPE = "Image"; // 	Binary data of an image;
    public static final String OPENHAB_LOCATION_TYPE = "Location"; // GPS coordinates;	Point
    public static final String OPENHAB_NUMBER_TYPE = "Number"; // Values in number format;	Decimal
    public static final String OPENHAB_PLAYER_TYPE = "Player"; // Allows control of players (e.g. audio players);	PlayPause, NextPrevious, RewindFastforward
    public static final String OPENHAB_ROLLERSHUTTER_TYPE = "Rollershutter"; // Roller shutter Item, typically used for blinds;	UpDown, StopMove, Percent
    public static final String OPENHAB_STRING_TYPE = "String"; // Stores texts;	String
    public static final String OPENHAB_SWITCH_TYPE = "Switch"; //Switch Item, typically used for lights (on/off);	OnOff

    public static String generateItemName(final UnitConfig unitConfig, final ServiceType serviceType) {
        // openHAB only supports underscores as special characters in item names
        return unitConfig.getAlias(0).replace(UnitAliasGenerationConsistencyHandler.ALIAS_NUMBER_SEPARATOR, ITEM_SUBSEGMENT_DELIMITER) +
                ITEM_SEGMENT_DELIMITER +
                StringProcessor.transformUpperCaseToCamelCase(serviceType.name());
    }

    public static OpenHABItemNameMetaData getMetaData(final String itemName) throws CouldNotPerformException {
        return new OpenHABItemNameMetaData(itemName);
    }

    public static String getItemType(final ServiceType serviceType) throws NotAvailableException {
        switch (serviceType) {
            case COLOR_STATE_SERVICE:
                return OPENHAB_COLOR_TYPE;
            case POWER_CONSUMPTION_STATE_SERVICE:
            case TEMPERATURE_STATE_SERVICE:
            case MOTION_STATE_SERVICE:
            case TAMPER_STATE_SERVICE:
            case BATTERY_STATE_SERVICE:
            case SMOKE_ALARM_STATE_SERVICE:
            case SMOKE_STATE_SERVICE:
            case TEMPERATURE_ALARM_STATE_SERVICE:
            case TARGET_TEMPERATURE_STATE_SERVICE:
            case ILLUMINANCE_STATE_SERVICE:
                return OPENHAB_NUMBER_TYPE;
            case BLIND_STATE_SERVICE:
                return OPENHAB_ROLLERSHUTTER_TYPE;
            case POWER_STATE_SERVICE:
            case BUTTON_STATE_SERVICE:
                return OPENHAB_SWITCH_TYPE;
            case CONTACT_STATE_SERVICE:
                return OPENHAB_CONTACT_TYPE;
            case HANDLE_STATE_SERVICE:
                return OPENHAB_STRING_TYPE;
            case BRIGHTNESS_STATE_SERVICE:
                return OPENHAB_DIMMER_TYPE;
            default:
                throw new NotAvailableException("OpenHAB item type for service[" + serviceType.name() + "]");
        }
    }

    public static class OpenHABItemNameMetaData {

        private final String alias;
        private final ServiceType serviceType;

        OpenHABItemNameMetaData(final String itemName) throws CouldNotPerformException {
            try {
                String[] nameSegment = itemName.split(ITEM_SEGMENT_DELIMITER);

                try {
                    alias = nameSegment[0].replace(ITEM_SUBSEGMENT_DELIMITER, UnitAliasGenerationConsistencyHandler.ALIAS_NUMBER_SEPARATOR);
                } catch (IndexOutOfBoundsException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not extract alias from item name!", ex);
                }

                try {
                    serviceType = ServiceType.valueOf(StringProcessor.transformToUpperCase(nameSegment[1]));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could not extract service type from item name!", ex);
                }
            } catch (CouldNotPerformException ex) {
                throw new CouldNotPerformException("Could not extract meta data out of item name[" + itemName + "]", ex);
            }
        }

        public ServiceType getServiceType() {
            return serviceType;
        }

        public String getAlias() {
            return alias;
        }
    }
}
