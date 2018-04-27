package org.openbase.bco.app.openhab.registry.synchronizer;

import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.processing.StringProcessor;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.unit.UnitConfigType.UnitConfig;

public class OpenHABItemHelper {

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
        return unitConfig.getAlias(0).replaceAll("-", "_") +
                "_" +
                StringProcessor.transformUpperCaseToCamelCase(serviceType.name());
    }

    public static String getItemType(final ServiceType serviceType, final ServicePattern servicePattern) throws NotAvailableException {
        switch (servicePattern) {
            case OPERATION:
                switch (serviceType) {
                    case COLOR_STATE_SERVICE:
                        return OPENHAB_COLOR_TYPE;
                    case BRIGHTNESS_STATE_SERVICE:
                        return OPENHAB_DIMMER_TYPE;
                    case POWER_STATE_SERVICE:
                        return OPENHAB_SWITCH_TYPE;
                }
        }
        throw new NotAvailableException("OpenHAB item type for service[" + serviceType.name() + ", " + servicePattern.name() + "]");
    }
}
