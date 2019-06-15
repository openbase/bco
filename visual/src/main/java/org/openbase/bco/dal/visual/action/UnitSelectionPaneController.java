package org.openbase.bco.dal.visual.action;

/*-
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2019 openbase.org
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

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openbase.bco.dal.visual.util.SelectorPanel.LocationUnitConfigHolder;
import org.openbase.bco.dal.visual.util.SelectorPanel.UnitConfigHolder;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.bco.registry.remote.login.BCOLogin;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.MultiException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.pattern.Filter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.service.ServiceConfigType.ServiceConfig;
import org.openbase.type.domotic.service.ServiceDescriptionType.ServiceDescription;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.EnablingStateType.EnablingState;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate;
import org.openbase.type.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UnitSelectionPaneController extends AbstractFXController {

    public final static LocationUnitConfigHolder ALL_LOCATION = new LocationUnitConfigHolder(null);
    public final static UnitTemplateHolder ALL_UNIT = new UnitTemplateHolder(null);
    public final static ServiceTemplateHolder ALL_SERVICE = new ServiceTemplateHolder(null);

    private static final Logger LOGGER = LoggerFactory.getLogger(UnitSelectionPaneController.class);
    private final ReentrantReadWriteLock updateComponentLock;
    private SimpleStringProperty unitIdProperty;
    private LocationUnitConfigHolder selectedLocationConfigHolder;
    private UnitConfigHolder selectedUnitConfigHolder;
    private ServiceTemplateHolder selectedServiceTemplateHolder;
    private UnitTemplateHolder selectedUnitTemplateHolder;

    private Filter<ServiceTemplate> serviceTemplateFilter;
    private Filter<UnitTemplate> unitTemplateFilter;
    private Filter<UnitConfig> locationFilter;
    private Filter<UnitConfig> unitFilter;

    @FXML
    private ComboBox<LocationUnitConfigHolder> locationComboBox;

    @FXML
    private ComboBox<UnitTemplateHolder> unitTemplateComboBox;

    @FXML
    private ComboBox<ServiceTemplateHolder> serviceTemplateComboBox;

    @FXML
    private ComboBox<UnitConfigHolder> unitComboBox;

    @FXML
    private AnchorPane mainPain;


    public UnitSelectionPaneController() {
        this.unitIdProperty = new SimpleStringProperty();
        this.updateComponentLock = new ReentrantReadWriteLock();
        final Observer dataObserver = (source, data) -> update();

        try {
            BCOLogin.getSession().autoLogin(true);
            Registries.getTemplateRegistry(false).addDataObserver(dataObserver);
            Registries.getUnitRegistry(false).addDataObserver(dataObserver);
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not register dynamic content observer!", ex, System.out);
        } catch (InterruptedException ex) {
            // skip observer registration on shutdown
        }
    }

    @Override
    public void initContent() throws InitializationException {
        try {
            locationComboBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                if (!locationComboBox.isDisabled()) {
                    updateDynamicContent();
                }
            }));

            unitTemplateComboBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                if (!unitTemplateComboBox.isDisabled()) {
                    updateDynamicContent();
                }
            }));

            serviceTemplateComboBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                if (!serviceTemplateComboBox.isDisabled()) {
                    updateDynamicContent();
                }
            }));

            unitComboBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
                if (!unitComboBox.isDisabled()) {
                    if (newValue == null) {
                        LOGGER.debug("ignore null value update!");
                        return;
                    }
                    unitIdProperty.set(newValue.getConfig().getId());
                }
            }));

            Registries.getUnitRegistry().addConnectionStateObserver((source, data) -> {
                mainPain.setDisable(data != State.CONNECTED);
                if (data != State.CONNECTED) {
                    mainPain.setStyle("-fx-color-label-visible: red");
                } else {
                    mainPain.setStyle("");
                }
            });

//        unitComboBox.getItems().filtered(unitConfigHolder -> {
//            if ((!locationComboBox.getSelectionModel().isEmpty()
//                    && locationComboBox.getSelectionModel().getSelectedIndex() != 0
//                    && !locationComboBox.getSelectionModel().getSelectedItem().getConfig().getId().equals(unitConfigHolder.getConfig().getPlacementConfig().getLocationId()))) {
//                return false;
//            }
//            if ((!unitTemplateComboBox.getSelectionModel().isEmpty()
//                    && unitTemplateComboBox.getSelectionModel().getSelectedIndex() != 0
//                    && !unitTemplateComboBox.getSelectionModel().getSelectedItem().getUnitTemplate().getType().equals(unitConfigHolder.getConfig().getUnitType()))) {
//                return false;
//            }
//            if (!serviceTemplateComboBox.getSelectionModel().isEmpty()
//                    && serviceTemplateComboBox.getSelectionModel().getSelectedIndex() != 0) {
//                boolean found = false;
//                final ServiceType selectedServicType = serviceTemplateComboBox.getSelectionModel().getSelectedItem().getUnitTemplate().getType();
//                for (ServiceConfig serviceConfig : unitConfigHolder.getConfig().getServiceConfigList()) {
//                    if (serviceConfig.getServiceDescription().getServiceType() == selectedServicType) {
//                        found = true;
//                        break;
//                    }
//                }
//                if (!found) {
//                    return false;
//                }
//            }
//            return true;
//        });
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void updateDynamicContent() {
        if (!Registries.isDataAvailable()) {
            locationComboBox.setDisable(true);
            unitTemplateComboBox.setDisable(true);
            serviceTemplateComboBox.setDisable(true);
            unitComboBox.setDisable(true);
            return;
        }

        MultiException.ExceptionStack exceptionStack = null;

        updateComponentLock.writeLock().lock();
        try {
            LOGGER.debug("Update selectorPanel!");
            // store selection to recover state after update
            try {
                selectedLocationConfigHolder = locationComboBox.getSelectionModel().getSelectedItem();
                if (selectedLocationConfigHolder == null) {
                    selectedLocationConfigHolder = ALL_LOCATION;
                }
            } catch (Exception ex) {
                selectedLocationConfigHolder = ALL_LOCATION;
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            }

            try {
                selectedUnitTemplateHolder = unitTemplateComboBox.getSelectionModel().getSelectedItem();
                if (selectedUnitTemplateHolder == null) {
                    selectedUnitTemplateHolder = ALL_UNIT;
                }
            } catch (Exception ex) {
                selectedUnitTemplateHolder = ALL_UNIT;
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            }

            try {
                selectedServiceTemplateHolder = serviceTemplateComboBox.getSelectionModel().getSelectedItem();
                if (selectedServiceTemplateHolder == null) {
                    selectedServiceTemplateHolder = ALL_SERVICE;
                }
            } catch (Exception ex) {
                selectedServiceTemplateHolder = ALL_SERVICE;
                ExceptionPrinter.printHistory(ex, LOGGER, LogLevel.ERROR);
            }

            try {
                selectedUnitConfigHolder = unitComboBox.getSelectionModel().getSelectedItem();
            } catch (Exception ex) {
                selectedUnitConfigHolder = null;
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            // update unit types
            try {
                ObservableList<UnitTemplateHolder> unitTemplateHolderList = FXCollections.observableArrayList();
                unitTemplateHolderList.add(ALL_UNIT);

                // apply service type filter if needed
                if (serviceTemplateComboBox.getSelectionModel().getSelectedItem() != null && !serviceTemplateComboBox.getSelectionModel().getSelectedItem().isNotSpecified()) {
                    final ServiceType serviceTypeFilter = serviceTemplateComboBox.getSelectionModel().getSelectedItem().getType();
                    for (final UnitTemplate unitTemplate : Registries.getTemplateRegistry().getUnitTemplates()) {

                        // apply custom filter
                        if (unitTemplateFilter != null && unitTemplateFilter.match(unitTemplate)) {
                            continue;
                        }

                        for (final ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
                            if (serviceDescription.getServiceType() == serviceTypeFilter) {
                                unitTemplateHolderList.add(new UnitTemplateHolder(unitTemplate));
                                break;
                            }
                        }
                    }
                } else {
                    for (UnitTemplate unitTemplate : Registries.getTemplateRegistry().getUnitTemplates()) {

                        // apply custom filter
                        if (unitTemplateFilter != null && unitTemplateFilter.match(unitTemplate)) {
                            continue;
                        }

                        unitTemplateHolderList.add(new UnitTemplateHolder(unitTemplate));
                    }
                }

                Collections.sort(unitTemplateHolderList);
                unitTemplateComboBox.setDisable(!false);
                unitTemplateComboBox.setItems(new SortedList<>(unitTemplateHolderList));
                unitTemplateComboBox.getSelectionModel().select(selectedUnitTemplateHolder);
                unitTemplateComboBox.setDisable(unitTemplateComboBox.getItems().size() <= 1);
            } catch (Exception ex) {
                unitTemplateComboBox.setDisable(!false);
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            // update service types
            try {

                // precompute location supported services
                final Set<ServiceType> locationSupportedServiceConfigList;
                if (!selectedLocationConfigHolder.isNotSpecified()) {
                    locationSupportedServiceConfigList = Registries.getUnitRegistry().getServiceTypesByLocation(selectedLocationConfigHolder.getConfig().getId());
                } else {
                    locationSupportedServiceConfigList = null;
                }

                // precompute unit supported services
                final Set<ServiceType> unitTypeSupportedServiceConfigList;
                if (unitTemplateComboBox.getSelectionModel().getSelectedItem() != null && !unitTemplateComboBox.getSelectionModel().getSelectedItem().isNotSpecified()) {
                    unitTypeSupportedServiceConfigList = new TreeSet<>();
                    for (final ServiceDescription serviceDescription : Registries.getTemplateRegistry().getUnitTemplateByType(unitTemplateComboBox.getSelectionModel().getSelectedItem().getType()).getServiceDescriptionList()) {
                        unitTypeSupportedServiceConfigList.add(serviceDescription.getServiceType());
                    }
                } else {
                    unitTypeSupportedServiceConfigList = null;
                }

                ObservableList<ServiceTemplateHolder> serviceTemplateHolderList = FXCollections.observableArrayList();
                serviceTemplateHolderList.add(ALL_SERVICE);
                for (ServiceTemplate serviceTemplate : Registries.getTemplateRegistry().getServiceTemplates()) {

                    // apply location type filter if needed
                    if (locationSupportedServiceConfigList != null && !locationSupportedServiceConfigList.contains(serviceTemplate.getServiceType())) {
                        continue;
                    }

                    // apply unit type filter if needed
                    if (unitTypeSupportedServiceConfigList != null && !unitTypeSupportedServiceConfigList.contains(serviceTemplate.getServiceType())) {
                        continue;
                    }

                    // apply custom filter
                    if (serviceTemplateFilter != null && serviceTemplateFilter.match(serviceTemplate)) {
                        continue;
                    }

                    serviceTemplateHolderList.add(new ServiceTemplateHolder(serviceTemplate));
                }

                Collections.sort(serviceTemplateHolderList);
                serviceTemplateComboBox.setDisable(!false);
                serviceTemplateComboBox.setItems(new SortedList<>(serviceTemplateHolderList));
                serviceTemplateComboBox.getSelectionModel().select(selectedServiceTemplateHolder);
                serviceTemplateComboBox.setDisable(serviceTemplateComboBox.getItems().size() <= 1);
            } catch (Exception ex) {
                locationComboBox.setDisable(!false);
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            // update location types
            try {
                ObservableList<LocationUnitConfigHolder> locationConfigHolderList = FXCollections.observableArrayList();
                locationConfigHolderList.add(ALL_LOCATION);
                for (UnitConfig locationUnitConfig : Registries.getUnitRegistry().getUnitConfigs(UnitType.LOCATION)) {

                    // apply custom filter
                    if (locationFilter != null && locationFilter.match(locationUnitConfig)) {
                        continue;
                    }

                    locationConfigHolderList.add(new LocationUnitConfigHolder(locationUnitConfig));
                }

                Collections.sort(locationConfigHolderList);
                locationComboBox.setDisable(!false);
                locationComboBox.setItems(new SortedList<>(locationConfigHolderList));
                locationComboBox.getSelectionModel().select(selectedLocationConfigHolder);
                locationComboBox.setDisable(locationComboBox.getItems().size() <= 1);

            } catch (CouldNotPerformException ex) {
                locationComboBox.setDisable(!false);
                ExceptionPrinter.printHistory(ex, LOGGER);
            }

            try {
                ObservableList<UnitConfigHolder> unitConfigHolderList = FXCollections.observableArrayList();
                UnitType selectedUnitType = unitTemplateComboBox.getSelectionModel().getSelectedItem().getType();
                ServiceType selectedServiceType = serviceTemplateComboBox.getSelectionModel().getSelectedItem().getType();

                // generate unit config list
                List<UnitConfig> selectedUnitConfigs = new ArrayList<>();
                if (selectedLocationConfigHolder != null && !selectedLocationConfigHolder.isNotSpecified()) {
                    selectedUnitConfigs.addAll(Registries.getUnitRegistry().getUnitConfigsByLocation(selectedLocationConfigHolder.getConfig().getId()));
                } else {
                    selectedUnitConfigs.addAll(Registries.getUnitRegistry().getUnitConfigs());
                }

                for (UnitConfig unitConfig : selectedUnitConfigs) {

                    // filter disabled units
                    if (unitConfig.getEnablingState().getValue() != EnablingState.State.ENABLED) {
                        continue;
                    }

                    if (unitConfig.getPlacementConfig().getLocationId().isEmpty()) {
                        LOGGER.warn("Could not load location unit of " + unitConfig.getLabel() + " because its location is not configured!");
                        continue;
                    }

                    // filter if unit or service type is not supported
                    if (unitTemplateComboBox.isDisabled() || serviceTemplateComboBox.isDisabled()) {
                        continue;
                    }

                    // filter units by selections
                    if (selectedUnitType != UnitType.UNKNOWN) {
                        if (unitConfig.getUnitType() != selectedUnitType) {
                            continue;
                        }
                    }

                    // filter service by selections
                    if (selectedServiceType != ServiceType.UNKNOWN) {
                        boolean found = false;
                        for (final ServiceConfig serviceConfig : unitConfig.getServiceConfigList()) {
                            if (serviceConfig.getServiceDescription().getServiceType() == selectedServiceType) {
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            continue;
                        }
                    }

                    // apply custom filter
                    if (unitFilter != null && unitFilter.match(unitConfig)) {
                        continue;
                    }

                    // generate config holder for unit
                    unitConfigHolderList.add(new UnitConfigHolder(unitConfig));
                }

                // sort units and setup model
                Collections.sort(unitConfigHolderList);
                unitComboBox.setItems(new SortedList<>(unitConfigHolderList));
                if (selectedUnitConfigHolder != null) {
                    unitComboBox.getSelectionModel().select(selectedUnitConfigHolder);
                }
                unitComboBox.setDisable(unitConfigHolderList.size() <= 0);

                if (selectedUnitType == UnitType.LOCATION) {
                    locationComboBox.getSelectionModel().select(0);
                    locationComboBox.setDisable(!false);
                }

                // auto select
                if (locationComboBox.getSelectionModel().getSelectedIndex() == -1) {
                    locationComboBox.getSelectionModel().select(0);
                }
                if (unitTemplateComboBox.getSelectionModel().getSelectedIndex() == -1) {
                    unitTemplateComboBox.getSelectionModel().select(0);
                }
                if (serviceTemplateComboBox.getSelectionModel().getSelectedIndex() == -1) {
                    serviceTemplateComboBox.getSelectionModel().select(0);
                }
                if (unitComboBox.getSelectionModel().getSelectedIndex() == -1) {
                    unitComboBox.getSelectionModel().select(0);
                }
            } catch (CouldNotPerformException ex) {
                unitComboBox.setDisable(!false);
                throw ex;
            }
            MultiException.checkAndThrow(() -> "Could not acquire all information!", exceptionStack);
        } catch (CouldNotPerformException | NullPointerException ex) {
        } finally {
            updateComponentLock.writeLock().unlock();
        }
    }

    public void setLocationFilter(Filter<UnitConfig> locationFilter) {
        this.locationFilter = locationFilter;
    }

    public void setUnitFilter(Filter<UnitConfig> unitFilter) {
        this.unitFilter = unitFilter;
    }

    public void setServiceTemplateFilter(Filter<ServiceTemplate> serviceTemplateFilter) {
        this.serviceTemplateFilter = serviceTemplateFilter;
    }

    public void setUnitTemplateFilter(Filter<UnitTemplate> unitTemplateFilter) {
        this.unitTemplateFilter = unitTemplateFilter;
    }

    public void setupServicePatternPass(final ServicePattern servicePattern) {

        // filter services which do not support the service pattern.
        setServiceTemplateFilter(serviceTemplate -> {
            try {
                return !Registries.getTemplateRegistry(false).validateServicePatternSupport(serviceTemplate.getServiceType(), servicePattern);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not compute match for service pattern filter!", ex, LOGGER, LogLevel.DEBUG);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return false;
        });

        // filter units which do not support the service pattern.
        setUnitTemplateFilter(unitTemplate -> {
            for (ServiceDescription serviceDescription : unitTemplate.getServiceDescriptionList()) {
                if (serviceDescription.getPattern() == servicePattern) {
                    return false;
                }
            }
            return true;
        });
    }

    /**
     * The property linking to the currently selected unit.
     *
     * @return the property holding the unit id.
     */
    public ReadOnlyStringProperty unitIdProperty() {
        return unitIdProperty;
    }

    public static class LocationUnitConfigHolder implements Comparable<LocationUnitConfigHolder> {

        private final UnitConfig locationUnitConfig;

        public LocationUnitConfigHolder(UnitConfig locationUnitConfig) {
            this.locationUnitConfig = locationUnitConfig;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "All";
            }
            return LabelProcessor.getBestMatch(locationUnitConfig.getLabel(), "?");
        }

        public boolean isNotSpecified() {
            return locationUnitConfig == null;
        }

        public UnitConfig getConfig() {
            return locationUnitConfig;
        }

        @Override
        public int compareTo(LocationUnitConfigHolder o) {
            if (o == null) {
                return -1;
            }

            // make sure "all" is on top.
            if (isNotSpecified()) {
                return -1;
            } else if (o.isNotSpecified()) {
                return +1;
            }

            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            LocationUnitConfigHolder instance = (LocationUnitConfigHolder) obj;

            // handle ALL entry
            if (locationUnitConfig == null && instance.locationUnitConfig == null) {
                return true;
            }
            if (locationUnitConfig == null || instance.locationUnitConfig == null) {
                return super.equals(obj);
            }

            return new EqualsBuilder()
                    .append(locationUnitConfig.getId(), instance.locationUnitConfig.getId())
                    .isEquals();
        }

        @Override
        public int hashCode() {

            // filter all entry location
            if (locationUnitConfig == null) {
                return super.hashCode();
            }

            return new HashCodeBuilder(17, 37).
                    append(locationUnitConfig.getId()).
                    toHashCode();
        }
    }

    public static class UnitTemplateHolder implements Comparable<UnitTemplateHolder> {

        private final UnitTemplate unitTemplate;

        public UnitTemplateHolder(final UnitTemplate unitTemplate) {
            this.unitTemplate = unitTemplate;
        }

        @Override
        public String toString() {
            if (getType().equals(UnitType.UNKNOWN)) {
                return "All";
            }
            return LabelProcessor.getBestMatch(unitTemplate.getLabel(), "?");
        }

        public boolean isNotSpecified() {
            return getType().equals(UnitType.UNKNOWN);
        }

        public UnitTemplate getUnitTemplate() {
            return unitTemplate;
        }

        public UnitType getType() {
            if (unitTemplate == null) {
                return UnitType.UNKNOWN;
            }
            return unitTemplate.getUnitType();
        }

        @Override
        public int compareTo(final UnitTemplateHolder o) {
            if (o == null) {
                return -1;
            }

            // make sure "all" is on top.
            if (getType().equals(UnitType.UNKNOWN)) {
                return -1;
            } else if (o.getType().equals(UnitType.UNKNOWN)) {
                return +1;
            }

            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            UnitTemplateHolder instance = (UnitTemplateHolder) obj;
            return new EqualsBuilder()
                    .append(getType(), instance.getType())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).
                    append(getType()).
                    toHashCode();
        }
    }

    public void setServiceFilter() {

    }

    public static class ServiceTemplateHolder implements Comparable<ServiceTemplateHolder> {

        private final ServiceTemplate serviceTemplate;

        public ServiceTemplateHolder(final ServiceTemplate template) {
            this.serviceTemplate = template;
        }

        @Override
        public String toString() {
            if (getType().equals(ServiceType.UNKNOWN)) {
                return "All";
            }
            return LabelProcessor.getBestMatch(serviceTemplate.getLabel(), "?");
        }

        public boolean isNotSpecified() {
            return getType().equals(ServiceType.UNKNOWN);
        }

        public ServiceType getType() {
            if (serviceTemplate == null) {
                return ServiceType.UNKNOWN;
            }
            return serviceTemplate.getServiceType();
        }

        public ServiceTemplate getServiceTemplate() {
            return serviceTemplate;
        }

        @Override
        public int compareTo(final ServiceTemplateHolder o) {
            if (o == null) {
                return -1;
            }

            // make sure "all" is on top.
            if (getType().equals(ServiceType.UNKNOWN)) {
                return -1;
            } else if (o.getType().equals(ServiceType.UNKNOWN)) {
                return +1;
            }

            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            ServiceTemplateHolder instance = (ServiceTemplateHolder) obj;
            return new EqualsBuilder()
                    .append(getType(), instance.getType())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).
                    append(getType()).
                    toHashCode();
        }
    }

    public static class UnitConfigHolder implements Comparable<UnitConfigHolder> {

        private final UnitConfig config;

        public UnitConfigHolder(final UnitConfig unitConfig) {
            this.config = unitConfig;
        }

        @Override
        public String toString() {
            if (isNotSpecified()) {
                return "Not Available";
            }

            String unitType = null;
            try {
                unitType = LabelProcessor.getBestMatch(Registries.getTemplateRegistry(false).getUnitTemplateByType(config.getUnitType()).getLabel(), "?");
            } catch (CouldNotPerformException | InterruptedException ex) {
                unitType = "?";
            }

            String label = LabelProcessor.getBestMatch(config.getLabel(), "?");

            String locationLabel = null;
            try {
                locationLabel = LabelProcessor.getBestMatch(Registries.getUnitRegistry(false).getUnitConfigById(config.getPlacementConfig().getLocationId(), UnitType.LOCATION).getLabel(), "?");
            } catch (CouldNotPerformException | InterruptedException ex) {
                unitType = "?";
            }

            final String description = MultiLanguageTextProcessor.getBestMatch(config.getDescription(), "");
            return unitType
                    + " = " + label + ""
                    + " @ " + locationLabel
                    + (description.isEmpty() ? "" : " (" + description + ")");
        }

        public boolean isNotSpecified() {
            return config == null;
        }

        public UnitConfig getConfig() {
            return config;
        }

        @Override
        public int compareTo(final UnitConfigHolder o) {
            if (o == null) {
                return -1;
            }
            return toString().compareTo(o.toString());
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            UnitConfigHolder instance = (UnitConfigHolder) obj;
            return new EqualsBuilder()
                    .append(config.getId(), instance.config.getId())
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).
                    append(config.getId()).
                    toHashCode();
        }
    }
}
