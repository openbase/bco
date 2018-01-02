
package org.openbase.bco.manager.device.binding.openhab.util.configgen;

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

import static org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ServiceItemEntry.SERVICE_TEMPLATE_BINDING_ICON;
import static org.openbase.bco.manager.device.binding.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.processing.VariableProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.openbase.bco.manager.device.binding.openhab.util.configgen.items.ItemIdGenerator;
import org.slf4j.LoggerFactory;
import rst.domotic.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class GroupEntry {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(GroupEntry.class);

    public static final String OPENHAB_ID_DELIMITER = "_";

    //Group LivingRoom        "Wohnzimmer"        <sofa>              (All,GroundFloor,Home)
    private final String groupId;
    private final String label;
    private final String icon;
    private final List<String> parentLocations;

    private static int maxGroupIdSize = 0;
    private static int maxLabelSize = 0;
    private static int maxIconSize = 0;
    private static int maxParentLocationsSize = 0;

    public GroupEntry(final UnitConfig locationUnitConfig) throws CouldNotPerformException, InterruptedException {
        this(ItemIdGenerator.generateUnitGroupID(locationUnitConfig), locationUnitConfig.getLabel(), detectIcon(new MetaConfigVariableProvider("LocationConfig", locationUnitConfig.getMetaConfig())), new ArrayList<>());
        if (!locationUnitConfig.getLocationConfig().getRoot()) {
            this.parentLocations.add(ItemIdGenerator.generateParentGroupID(locationUnitConfig));
        }
        calculateGaps();
    }

    public GroupEntry(final String groupId, final String label, final String icon, final GroupEntry parent) {
        this(groupId, label, icon, new ArrayList<>());
        this.parentLocations.add(parent.getGroupId());
        calculateGaps();
    }

    public GroupEntry(final String groupId, final String label, final String icon, final List<String> parentLocations) {
        this.groupId = groupId;
        this.label = label;
        this.icon = icon;
        this.parentLocations = parentLocations;
        calculateGaps();
    }

    public static String detectIcon(final VariableProvider variableProvider) {
        try {
            return variableProvider.getValue(SERVICE_TEMPLATE_BINDING_ICON);
        } catch (NotAvailableException ex) {
            return "corridor";
        }
    }

    private void calculateGaps() {
        maxGroupIdSize = Math.max(maxGroupIdSize, getGroupIdStringRep().length());
        maxLabelSize = Math.max(maxLabelSize, getLabelStringRep().length());
        maxIconSize = Math.max(maxIconSize, getIconStringRep().length());
        maxParentLocationsSize = Math.max(maxParentLocationsSize, getParentLocationsStringRep().length());
    }

    public String getGroupId() {
        return groupId;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public List<String> getParentLocations() {
        return Collections.unmodifiableList(parentLocations);
    }

    public String getGroupIdStringRep() {
        return groupId;
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

    public String getParentLocationsStringRep() {
        if (parentLocations.isEmpty()) {
            return "";
        }
        String stringRep = "(";
        boolean firstIteration = true;
        for (String parent : parentLocations) {
            if (!firstIteration) {
                stringRep += ",";
            } else {
                firstIteration = false;
            }
            stringRep += parent;
        }
        stringRep += ")";
        return stringRep;
    }

    public String buildStringRep() {

        String stringRep = "Group" + StringProcessor.fillWithSpaces("", TAB_SIZE);

        // group id
        stringRep += StringProcessor.fillWithSpaces(getGroupIdStringRep(), maxGroupIdSize + TAB_SIZE);

        // label
        stringRep += StringProcessor.fillWithSpaces(getLabelStringRep(), maxLabelSize + TAB_SIZE);

        // icon
        stringRep += StringProcessor.fillWithSpaces(getIconStringRep(), maxIconSize + TAB_SIZE);

        // parent locations
        stringRep += StringProcessor.fillWithSpaces(getParentLocationsStringRep(), maxParentLocationsSize + TAB_SIZE);

        return stringRep;
    }

    public static void reset() {
        maxGroupIdSize = 0;
        maxLabelSize = 0;
        maxIconSize = 0;
        maxParentLocationsSize = 0;
    }
}
