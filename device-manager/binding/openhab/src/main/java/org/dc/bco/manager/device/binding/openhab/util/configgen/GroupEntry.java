
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.device.binding.openhab.util.configgen;

import static org.dc.bco.manager.device.binding.openhab.util.configgen.ItemEntry.SERVICE_TEMPLATE_BINDING_ICON;
import static org.dc.bco.manager.device.binding.openhab.util.configgen.OpenHABItemConfigGenerator.TAB_SIZE;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.NotAvailableException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.extension.rsb.scope.ScopeGenerator;
import org.dc.jul.extension.rst.processing.MetaConfigVariableProvider;
import org.dc.jul.processing.StringProcessor;
import org.dc.jul.processing.VariableProvider;
import org.dc.bco.registry.location.remote.LocationRegistryRemote;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.LoggerFactory;
import rst.rsb.ScopeType.Scope;
import rst.spatial.LocationConfigType.LocationConfig;
import rst.spatial.PlacementConfigType.PlacementConfig;

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

    public GroupEntry(final LocationConfig locationConfig, final LocationRegistryRemote locationRegistryRemote) throws CouldNotPerformException {
        this(generateGroupID(locationConfig), locationConfig.getLabel(), detectIcon(new MetaConfigVariableProvider("LocationConfig", locationConfig.getMetaConfig())), new ArrayList<>());
        if (!locationConfig.getRoot()) {
            this.parentLocations.add(generateParentGroupID(locationConfig, locationRegistryRemote));
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

    public static String generateParentGroupID(final LocationConfig childLocationConfig, final LocationRegistryRemote locationRegistryRemote) throws CouldNotPerformException {

        try {
            return generateGroupID(childLocationConfig.getPlacementConfig().getLocationId(), locationRegistryRemote);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could location parent id via placement config. Outdated registry entry!", ex), logger);
            return generateGroupID(childLocationConfig.getPlacementConfig().getLocationId(), locationRegistryRemote);
        }

    }

    public static String generateGroupID(final PlacementConfig placementConfig, final LocationRegistryRemote locationRegistryRemote) throws CouldNotPerformException {
        String locationID;
        try {
            if (!placementConfig.hasLocationId()) {
                throw new NotAvailableException("placementconfig.locationid");
            }
            locationID = placementConfig.getLocationId();
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate group id!", ex);
        }
        return generateGroupID(locationID, locationRegistryRemote);
    }

    public static String generateGroupID(final String locationId, final LocationRegistryRemote locationRegistryRemote) throws CouldNotPerformException {
        LocationConfig locationConfig;
        try {
            locationConfig = locationRegistryRemote.getLocationConfigById(locationId);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate group id for LocationId[" + locationId + "]!", ex);
        }
        return generateGroupID(locationConfig);
    }

    public static String generateGroupID(final LocationConfig config) throws CouldNotPerformException {
        try {
            if (!config.hasScope()) {
                throw new NotAvailableException("locationconfig.scope");
            }
            return generateGroupID(config.getScope());
        } catch (NotAvailableException ex) {
            throw new CouldNotPerformException("Could not generate group id out of LocationConfig[" + config.getId() + "] !", ex);
        }
    }

    public static String generateGroupID(final Scope scope) throws CouldNotPerformException {
        try {
            if (scope == null) {
                throw new NotAvailableException("locationconfig.scope");
            }
            return ScopeGenerator.generateStringRepWithDelimiter(scope, OPENHAB_ID_DELIMITER);
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not generate group id out of Scope[" + scope + "]!", ex);
        }
    }
}
