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
import rst.spatial.LocationConfigType.LocationConfig;

/**
 *
 * @author Divine <a href="mailto:DivineThreepwood@gmail.com">Divine</a>
 */
public class GroupEntry {

    //Group LivingRoom        "Wohnzimmer"        <sofa>              (All,GroundFloor,Home)
    private final String groupId;
    private final String label;
    private final String icon;
    private final List<String> parentLocations;

    private static int maxGroupIdSize = 0;
    private static int maxLabelSize = 0;
    private static int maxIconSize = 0;
    private static int maxParentLocationsSize = 0;

    public GroupEntry(LocationConfig locationConfig) {
        this.groupId = locationConfig.getId();
        this.label = locationConfig.getLabel();
        this.icon = "sun";
        this.parentLocations = new ArrayList<>();
        if (!locationConfig.getRoot()) {
            this.parentLocations.add(locationConfig.getParentId());
        }
        calculateGaps();
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
        if(label.isEmpty()) {
            return "";
        }
        return "\"" + label + "\"";
    }

    public String getIconStringRep() {
        if(icon.isEmpty()) {
            return "";
        }
        return "<" + icon + ">";
    }

    public String getParentLocationsStringRep() {
        if(parentLocations.isEmpty()) {
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
