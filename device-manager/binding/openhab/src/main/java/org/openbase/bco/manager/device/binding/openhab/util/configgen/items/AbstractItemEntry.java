package org.openbase.bco.manager.device.binding.openhab.util.configgen.items;

/*
 * #%L
 * BCO Manager Device Binding OpenHAB
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.processing.StringProcessor;
import org.slf4j.LoggerFactory;
import rst.domotic.service.ServiceConfigType.ServiceConfig;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BATTERY_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BLIND_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BRIGHTNESS_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.BUTTON_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.COLOR_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.CONTACT_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.HANDLE_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.ILLUMINANCE_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.INTENSITY_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.MOTION_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_ALARM_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.SMOKE_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.TAMPER_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.TARGET_TEMPERATURE_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_ALARM_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.TEMPERATURE_STATE_SERVICE;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

/**
 *
 * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public abstract class AbstractItemEntry implements ItemEntry, Comparable<AbstractItemEntry> {

    protected static final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractItemEntry.class);

    public static final String ITEM_SUBSEGMENT_DELIMITER = "_";
    public static final String ITEM_SEGMENT_DELIMITER = "__";

    protected String commandType;
    protected String itemId;
    protected String label;
    protected String icon;
    protected final List<String> groups;
    protected String itemHardwareConfig;
    protected final UnitConfig unitConfig;
    protected final ServiceConfig serviceConfig;

    private static int maxCommandTypeSize = 0;
    private static int maxItemIdSize = 0;
    private static int maxLabelSize = 0;
    private static int maxIconSize = 0;
    private static int maxGroupSize = 0;
    private static int maxBindingConfigSize = 0;

    public AbstractItemEntry(UnitConfig unitConfig, ServiceConfig serviceConfig) {
        this.groups = new ArrayList<>();
        this.unitConfig = unitConfig;
        this.serviceConfig = serviceConfig;
    }

    protected void calculateGaps() {
        maxCommandTypeSize = Math.max(maxCommandTypeSize, getCommandTypeStringRep().length());
        maxItemIdSize = Math.max(maxItemIdSize, getItemIdStringRep().length());
        maxLabelSize = Math.max(maxLabelSize, getLabelStringRep().length());
        maxIconSize = Math.max(maxIconSize, getIconStringRep().length());
        maxGroupSize = Math.max(maxGroupSize, getGroupsStringRep().length());
        maxBindingConfigSize = Math.max(maxBindingConfigSize, getBindingConfigStringRep().length());
    }

    public static void reset() {
        maxCommandTypeSize = 0;
        maxItemIdSize = 0;
        maxLabelSize = 0;
        maxIconSize = 0;
        maxGroupSize = 0;
        maxBindingConfigSize = 0;
    }

    @Override
    public String buildStringRep() throws CouldNotPerformException {

        verify();

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

        // binding config
        stringRep += StringProcessor.fillWithSpaces(getBindingConfigStringRep(), maxBindingConfigSize + TAB_SIZE);

        return stringRep;
    }

    @Override
    public String getCommandType() {
        return commandType;
    }

    @Override
    public String getItemId() {
        return itemId;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public List<String> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    @Override
    public String getItemHardwareConfig() {
        return itemHardwareConfig;
    }

    @Override
    public String getCommandTypeStringRep() {
        return commandType;
    }

    @Override
    public String getItemIdStringRep() {
        return itemId;
    }

    @Override
    public String getLabelStringRep() {
        if (label.isEmpty()) {
            return "";
        }
        return "\"" + label + "\"";
    }

    @Override
    public String getIconStringRep() {
        if (icon.isEmpty()) {
            return "";
        }
        return "<" + icon + ">";
    }

    @Override
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

    @Override
    public String getBindingConfigStringRep() {
        return "{ " + itemHardwareConfig + " }";
    }

    protected String getDefaultCommand(ServiceTemplate.ServiceType serviceType) {
        switch (serviceType) {
            case COLOR_STATE_SERVICE:
                return "Color";
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
                return "Number";
            case BLIND_STATE_SERVICE:
                return "Rollershutter";
            case POWER_STATE_SERVICE:
            case BUTTON_STATE_SERVICE:
                return "Switch";
            case INTENSITY_STATE_SERVICE:
                return "Dimmer";
            case CONTACT_STATE_SERVICE:
                return "Contact";
            case HANDLE_STATE_SERVICE:
                return "String";
            case BRIGHTNESS_STATE_SERVICE:
                return "Dimmer";
            default:
                logger.warn("Unkown Service Type: " + serviceType);
                return "";
        }
    }

    public UnitConfig getUnitConfig() {
        return unitConfig;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    private void verify() throws VerificationFailedException {
        if (itemId.contains("-")) {
            throw new VerificationFailedException("Found \"-\" in item id which is not allowed for openhab configurations!");
        }
    }

    @Override
    public int compareTo(AbstractItemEntry o) {

        // unit type sort
        if (!getUnitTypeOrder(unitConfig.getType()).equals(getUnitTypeOrder(o.unitConfig.getType()))) {
            return getUnitTypeOrder(unitConfig.getType()).compareTo(getUnitTypeOrder(o.unitConfig.getType()));
        }

        // command type sort
        if (!getCommandType().equals(o.getCommandType())) {
            return getCommandType().compareTo(o.getCommandType());
        }

        // label sort
        return getLabel().compareTo(o.getLabel());
    }

    private Integer getUnitTypeOrder(final UnitType unitType) {
        switch (unitType) {
            case LOCATION:
                return 0;
            case SCENE:
                return 1;
            case UNIT_GROUP:
                return 2;
            default:
                return 100;
        }
    }
}
