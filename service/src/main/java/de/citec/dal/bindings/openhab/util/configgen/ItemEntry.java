/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen;

import static de.citec.dal.bindings.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import de.citec.jul.processing.StringProcessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.LoggerFactory;
import rst.homeautomation.service.OpenHABBindingServiceConfigType.OpenHABBindingServiceConfig;
import rst.homeautomation.service.ServiceConfigType.ServiceConfig;
import rst.homeautomation.service.ServiceTypeHolderType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author mpohling
 */
public class ItemEntry {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ItemEntry.class);

    private final String commandType;
    private final String itemId;
    private final String label;
    private final String icon;
    private final List<String> groups;
    private final String hardwareConfig;

    private static int maxCommandTypeSize = 0;
    private static int maxItemIdSize = 0;
    private static int maxLabelSize = 0;
    private static int maxIconSize = 0;
    private static int maxGroupSize = 0;
    private static int maxHardwareConfigSize = 0;

    public ItemEntry(final UnitConfig unitConfig, final ServiceConfig serviceConfig, final OpenHABBindingServiceConfig openHABBindingServiceConfig) {
        this.commandType = getCommand(serviceConfig.getType());
        this.itemId = openHABBindingServiceConfig.getItemId();
        this.label = unitConfig.getLabel();
        this.icon = "sun";
        this.groups = new ArrayList<>();

        // TODO: maybe think of another strategy to name groups
        // Dimmer and Rollershutter are key words in the openhab config and therefor cannot be used in groups
        String templateName = StringProcessor.transformUpperCaseToCamelCase(unitConfig.getTemplate().getType().name());
        if (!(templateName.equals("Dimmer") || templateName.equals("Rollershutter"))) {
            this.groups.add(StringProcessor.transformUpperCaseToCamelCase(unitConfig.getTemplate().getType().name()));
        }
        this.groups.add(StringProcessor.transformUpperCaseToCamelCase(serviceConfig.getType().name()));
        this.groups.add(unitConfig.getPlacementConfig().getLocationId());
        this.hardwareConfig = openHABBindingServiceConfig.getItemHardwareConfig();
        this.calculateGaps();
    }

    private void calculateGaps() {
        maxCommandTypeSize = Math.max(maxCommandTypeSize, getCommandTypeStringRep().length());
        maxItemIdSize = Math.max(maxItemIdSize, getItemIdStringRep().length());
        maxLabelSize = Math.max(maxLabelSize, getLabelStringRep().length());
        maxIconSize = Math.max(maxIconSize, getIconStringRep().length());
        maxGroupSize = Math.max(maxGroupSize, getGroupsStringRep().length());
        maxHardwareConfigSize = Math.max(maxHardwareConfigSize, getHardwareConfigStringRep().length());
    }

    public static void reset() {
        maxCommandTypeSize = 0;
        maxItemIdSize = 0;
        maxLabelSize = 0;
        maxIconSize = 0;
        maxGroupSize = 0;
        maxHardwareConfigSize = 0;
    }

    public String getCommandType() {
        return commandType;
    }

    public String getItemId() {
        return itemId;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public String getHardwareConfig() {
        return hardwareConfig;
    }

    public String getCommandTypeStringRep() {
        return commandType;
    }

    public String getItemIdStringRep() {
        return itemId;
    }

    public String getLabelStringRep() {
        if (label.isEmpty()) {
            return "";
        }
        return "\"" + label + "\"";
    }

    public String getIconStringRep() {
        if (icon.isEmpty()) {
            return "";
        }
        return "<" + icon + ">";
    }

    public String getGroupsStringRep() {
        if (groups.isEmpty()) {
            return "";
        }
        String stringRep = "(";
        boolean firstIteration = true;
        for (String group : groups) {
            if (!firstIteration) {
                stringRep += ",";
            } else {
                firstIteration = false;
            }
            stringRep += group;
        }
        stringRep += ")";
        return stringRep;
    }

    public String getHardwareConfigStringRep() {
        return "{ " + hardwareConfig + " }";
    }

    public String buildStringRep() {

        String stringRep = "";

        // command type
        stringRep += StringProcessor.fillWithSpaces(getCommandTypeStringRep(), maxCommandTypeSize + TAB_SIZE);

        // unit id
        stringRep += StringProcessor.fillWithSpaces(getItemIdStringRep(), maxItemIdSize + TAB_SIZE);

        // label
        stringRep += StringProcessor.fillWithSpaces(getLabelStringRep(), maxLabelSize + TAB_SIZE);

        // icon
        stringRep += StringProcessor.fillWithSpaces(getIconStringRep(), maxIconSize + TAB_SIZE);

        // groups
        stringRep += StringProcessor.fillWithSpaces(getGroupsStringRep(), maxGroupSize + TAB_SIZE);

        // hardware
        stringRep += StringProcessor.fillWithSpaces(getHardwareConfigStringRep(), maxHardwareConfigSize + TAB_SIZE);

        return stringRep;
    }

    private String getCommand(ServiceTypeHolderType.ServiceTypeHolder.ServiceType type) {
        switch (type) {
            case COLOR_SERVICE:
                return "Color";
            case OPENING_RATIO_PROVIDER:
            case POWER_CONSUMPTION_PROVIDER:
                return "Number";
            case BATTERY_PROVIDER:
                return "Percent";
            case SHUTTER_PROVIDER:
            case SHUTTER_SERVICE:
                return "Rollershutter";
            case POWER_SERVICE:
            case POWER_PROVIDER:
            case BUTTON_PROVIDER:
                return "Switch";
            case TEMPERATURE_PROVIDER:
            case MOTION_PROVIDER:
            case TAMPER_PROVIDER:
            case BRIGHTNESS_PROVIDER:
                return "Number";
            case BRIGHTNESS_SERVICE:
            case DIM_PROVIDER:
            case DIM_SERVICE:
                return "Dimmer";
            default:
                logger.warn("Unkown Service Type: " + type);
                return "";
        }
    }
}
