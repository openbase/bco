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

import com.google.protobuf.Message;
import javafx.application.Platform;
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
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.FatalImplementationErrorException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.openbase.jul.visual.javafx.fxml.FXMLProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;
import org.openbase.type.domotic.state.ActionStateType.ActionState.State;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
    private TableColumn interruptibleColumn;

    @FXML
    private TableColumn schedulableColumn;

    @FXML
    private TableColumn lifetimeColumn;

    @FXML
    private TableColumn executionTimeColumn;

    @FXML
    private TableColumn timestampColumn;

    @FXML
    private TableColumn validColumn;

    @FXML
    private TableColumn runningColumn;

    @FXML
    private TableColumn doneColumn;

    @FXML
    private TableColumn expiredColumn;



    private Observer unitObserver;
    private Unit<?> unit;
    private final ObservableList<UnitAllocationBean> data;

    public UnitAllocationPane() {
        this.unitIdProperty = new SimpleStringProperty();
        this.data = FXCollections.observableArrayList();
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
            validColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));
            runningColumn.setCellValueFactory(new PropertyValueFactory<>("running"));
            doneColumn.setCellValueFactory(new PropertyValueFactory<>("done"));
            expiredColumn.setCellValueFactory(new PropertyValueFactory<>("expired"));
            categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
            actionStateColumn.setCellValueFactory(new PropertyValueFactory<>("actionState"));
            actionIdColumn.setCellValueFactory(new PropertyValueFactory<>("actionId"));
            timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            lifetimeColumn.setCellValueFactory(new PropertyValueFactory<>("lifetime"));
            serviceStateColumn.setCellValueFactory(new PropertyValueFactory<>("serviceState"));
            initiatorColumn.setCellValueFactory(new PropertyValueFactory<>("initiator"));
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            interruptibleColumn.setCellValueFactory(new PropertyValueFactory<>("interruptible"));
            schedulableColumn.setCellValueFactory(new PropertyValueFactory<>("schedulable"));
            executionTimeColumn.setCellValueFactory(new PropertyValueFactory<>("executionTime"));

            UnitSelectionPaneControllerPair.getValue().unitIdProperty().addListener((a, b, c) -> {
                unitIdProperty.set(c);
            });

            actionTable.setItems(data);

            GlobalScheduledExecutorService.scheduleAtFixedRate(() -> Platform.runLater(() -> {
                actionTable.refresh();
            }),1, 1, TimeUnit.SECONDS);

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

        data.clear();

        try {
            int i = 1;
            for (ActionDescription actionDescription : unit.getActionList()) {
                data.add(new UnitAllocationBean(i++, actionDescription));
            }
        } catch (NotAvailableException ex) {
            ExceptionPrinter.printHistory("Could updated dynamic content!", ex, LOGGER);
        }
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
            // just do nothing to speed up the shutdown.
        }
    }

    public static class UnitAllocationBean {

        private final DateFormat dateFormat = DateFormat.getInstance();


        private final int position;
        private final String timestamp;
        private final String actionId;
        private final String serviceState;
        private final String initiator;
        private final String priority;
        private final String category;
        private final String description;
        private final boolean interruptible;
        private final boolean schedulable;

        private final RemoteAction remoteAction;

        private final static ProtoBufJSonProcessor protoBufJSonProcessor = new ProtoBufJSonProcessor();

        public UnitAllocationBean(final int position, final ActionDescription actionDescription) throws NotAvailableException {
            this.remoteAction = new RemoteAction(actionDescription);
            this.position = position;
            this.timestamp = dateFormat.format(new Date(TimestampJavaTimeTransform.transform(actionDescription.getTimestamp())));
            this.actionId = actionDescription.getId();
            this.description = MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription(),"?");
            this.priority = actionDescription.getPriority().name();
            this.category = StringProcessor.transformCollectionToString(actionDescription.getCategoryList(), ", ");
            this.interruptible = actionDescription.getInterruptible();
            this.schedulable = actionDescription.getSchedulable();

            final ActionInitiator actionInitiator = ActionDescriptionProcessor.getInitialInitiator(actionDescription);

            String initiatorLabel;
            try {
                initiatorLabel = Registries.getUnitRegistry().getUnitConfigById(actionInitiator.getInitiatorId()).getUserConfig().getUserName();
            } catch (CouldNotPerformException e) {
                initiatorLabel = "?";
            }

            this.initiator = initiatorLabel + " (" + actionInitiator.getInitiatorType().name() + " )";

            String tmpServiceState;
            try {
                final Message serviceStateMessage = protoBufJSonProcessor.deserialize(actionDescription.getServiceStateDescription().getServiceState(), actionDescription.getServiceStateDescription().getServiceStateClassName());
                tmpServiceState = StringProcessor.transformCollectionToString(Services.resolveStateValue(serviceStateMessage), ", ");
            } catch (CouldNotPerformException e) {
                tmpServiceState = "?";
            }
            this.serviceState = tmpServiceState;

        }

        public int getPosition() {
            return position;
        }

        public String getActionState() {
            return remoteAction.getActionState().name();
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

        public boolean isValid() {
            return remoteAction.isValid();
        }

        public boolean isRunning() {
            return remoteAction.isRunning();
        }

        public boolean isDone() {
            return remoteAction.isDone();
        }

        public boolean isExpired() {
            return remoteAction.isExpired();
        }

        public String getExecutionTime() {
            try {
                final long executionTime = remoteAction.getExecutionTime();
                final long days = TimeUnit.MILLISECONDS.toDays(executionTime);
                final long hours = TimeUnit.MILLISECONDS.toHours(executionTime);
                final long minutes = TimeUnit.MILLISECONDS.toMinutes(executionTime);
                final long seconds = TimeUnit.MILLISECONDS.toSeconds(executionTime);

                if(days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                    hours - TimeUnit.DAYS.toHours(days),
                    minutes - TimeUnit.HOURS.toMinutes(hours),
                    seconds - TimeUnit.MINUTES.toSeconds(minutes))
                    + (days > 0 ? " ( + "+ days +" days)" : "");
            } catch (NotAvailableException e) {
                return "?";
            }
        }

        public String getLifetime() {
            try {
                final long lifetime = remoteAction.getLifetime();
                final long days = TimeUnit.MILLISECONDS.toDays(lifetime);
                final long hours = TimeUnit.MILLISECONDS.toHours(lifetime);
                final long minutes = TimeUnit.MILLISECONDS.toMinutes(lifetime);
                final long seconds = TimeUnit.MILLISECONDS.toSeconds(lifetime);

                if(days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                        hours - TimeUnit.DAYS.toHours(days),
                        minutes - TimeUnit.HOURS.toMinutes(hours),
                        seconds - TimeUnit.MINUTES.toSeconds(minutes))
                        + (days > 0 ? " ( + "+ days +" days)" : "");
            } catch (NotAvailableException e) {
                return "?";
            }
        }

        public boolean getInterruptible() {
            return interruptible;
        }

        public boolean getSchedulable() {
            return schedulable;
        }
    }
}
