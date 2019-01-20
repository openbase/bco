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

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.openbase.jul.visual.javafx.fxml.FXMLProcessor;
import org.openbase.type.domotic.state.PresenceStateType.PresenceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;

import java.text.DateFormat;
import java.util.Date;

public class UnitAllocationPane extends AbstractFXController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAllocationPane.class);
    private SimpleStringProperty unitIdProperty;

    @FXML
    private HBox topPane;

    @FXML
    private TableView actionTable;

    @FXML
    private TableColumn positionColumn;

    @FXML
    private TableColumn actionStateColumn;

    @FXML
    private TableColumn serviceStateColumn;

    @FXML
    private TableColumn initiatorColumn;

    @FXML
    private TableColumn priorityColumn;

    @FXML
    private TableColumn categoryColumn;

    @FXML
    private TableColumn descriptionColumn;

    @FXML
    private TableColumn actionIdColumn;

    @FXML
    private TableColumn timestampColumn;

    private Observer unitObserver;
    private Unit<?> unit;

    public UnitAllocationPane() {
        this.unitIdProperty = new SimpleStringProperty();
        this.unitIdProperty.addListener((observable, oldValue, newValue) -> {
            System.out.println("update unit to: " + newValue);
            try {
                setUnitId(newValue);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not apply property change!", ex, LOGGER);
            }
        });
        this.unitObserver = (source, data) -> {
            update();
        };
    }

    @Override
    public void initContent() throws InitializationException {
        try {
            final Pair<Pane, UnitSelectionPane> UnitSelectionPaneControllerPair = FXMLProcessor.loadFxmlPaneAndControllerPair(UnitSelectionPane.class);
            topPane.getChildren().add(UnitSelectionPaneControllerPair.getKey());
            positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
            priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
            categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
            actionStateColumn.setCellValueFactory(new PropertyValueFactory<>("actionState"));
            actionIdColumn.setCellValueFactory(new PropertyValueFactory<>("actionId"));
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            serviceStateColumn.setCellValueFactory(new PropertyValueFactory<>("serviceState"));
            initiatorColumn.setCellValueFactory(new PropertyValueFactory<>("initiator"));
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
//            unitIdProperty.bind(UnitSelectionPaneControllerPair.getValue().unitIdProperty());
            UnitSelectionPaneControllerPair.getValue().unitIdProperty().addListener((a, b, c) -> {
                System.out.println("prop:" + c);
                unitIdProperty.set(c);
            });
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void updateDynamicContent() {

        // skip update if unit is not ready
        if (unit == null || !unit.isDataAvailable()) {
            return;
        }

        ObservableList<UnitAllocationBean> data = FXCollections.observableArrayList();
        try {
            int i = 1;
            for (ActionDescription actionDescription : unit.getActionList()) {
                data.add(new UnitAllocationBean(i++, actionDescription));
            }
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Could updated dynamic content!", ex, LOGGER);
        }
        actionTable.setItems(data);

    }

    private void setUnitId(final String unitId) throws CouldNotPerformException {
        try {
            if (unit != null) {
                unit.removeDataObserver(unitObserver);
            }
            unit = Units.getUnit(unitId, false);
            unit.addDataObserver(unitObserver);
            if (unit.isDataAvailable()) {
                update();
            }
        } catch (NotAvailableException e) {
            throw new CouldNotPerformException("Could not setup Unit[" + unitId + "]");
        } catch (InterruptedException e) {
            new FatalImplementationErrorException(this, e);
        }
    }

    public SimpleStringProperty unitIdProperty() {
        return unitIdProperty;
    }

    public static class UnitAllocationBean {

        private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM);


        private final int position;
        private final State actionState;
        private final String timestamp;
        private final String actionId;
        private final String serviceState;
        private final String initiator;
        private final String priority;
        private final String category;
        private final String description;

        public UnitAllocationBean(final int position, final ActionDescription actionDescription) throws NotAvailableException {
            this.position = position;
            this.actionState = actionDescription.getActionState().getValue();
            this.timestamp = dateFormat.format(new Date(TimestampJavaTimeTransform.transform(actionDescription.getTimestamp())));
            this.actionId = actionDescription.getId();
            this.serviceState = actionDescription.getServiceStateDescription().getServiceAttribute();
            this.description = MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription(),"?");
            this.priority = actionDescription.getPriority().name();
            this.category = StringProcessor.transformCollectionToString(actionDescription.getCategoryList(), ", ");
            final ActionInitiator actionInitiator = ActionDescriptionProcessor.getInitialInitiator(actionDescription);

            String initiatorLabel;
            try {
                initiatorLabel = LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionInitiator.getInitiatorId()).getLabel(), "?");
            } catch (CouldNotPerformException e) {
                initiatorLabel = "?";
            }

            this.initiator = initiatorLabel + " (" + actionInitiator.getInitiatorType().name() + " )";

        }

        public int getPosition() {
            return position;
        }

        public State getActionState() {
            return actionState;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getActionId() {
            return actionId;
        }

        public String getServiceState() {
            return serviceState;
        }

        public String getInitiator() {
            return initiator;
        }

        public String getCategory() {
            return category;
        }

        public String getDescription() {
            return description;
        }

        public String getPriority() {
            return priority;
        }
    }
}
