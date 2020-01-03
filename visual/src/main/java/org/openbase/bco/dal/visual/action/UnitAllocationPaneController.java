package org.openbase.bco.dal.visual.action;

/*-
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.layer.service.Services;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.lib.layer.unit.UnitRemote;
import org.openbase.bco.dal.lib.layer.unit.user.User;
import org.openbase.bco.dal.remote.action.RemoteAction;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.extension.protobuf.processing.ProtoBufJSonProcessor;
import org.openbase.jul.extension.type.processing.LabelProcessor;
import org.openbase.jul.extension.type.processing.MultiLanguageTextProcessor;
import org.openbase.jul.extension.type.processing.TimestampJavaTimeTransform;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.controller.Remote;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.jul.schedule.GlobalScheduledExecutorService;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.openbase.jul.visual.javafx.fxml.FXMLProcessor;
import org.openbase.type.domotic.action.ActionReferenceType.ActionReference;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServicePattern;
import org.openbase.type.domotic.state.ConnectionStateType.ConnectionState.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openbase.type.domotic.action.ActionDescriptionType.ActionDescription;
import org.openbase.type.domotic.action.ActionInitiatorType.ActionInitiator;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UnitAllocationPaneController extends AbstractFXController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnitAllocationPaneController.class);
    private SimpleStringProperty unitIdProperty;

    @FXML
    private HBox topPane;

    @FXML
    private TableView actionTable;

    @FXML
    private TableView actionChainTable;

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
    private TableColumn lastExtensionColumn;

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

    @FXML
    private TableColumn validityTimeColumn;

    @FXML
    private TableColumn actionChainPositionColumn;

    @FXML
    private TableColumn actionChainActionStateColumn;

    @FXML
    private TableColumn actionChainServiceStateColumn;

    @FXML
    private TableColumn actionChainInitiatorColumn;

    @FXML
    private TableColumn actionChainPriorityColumn;

    @FXML
    private TableColumn actionChainCategoryColumn;

    @FXML
    private TableColumn actionChainActionIdColumn;

    @FXML
    private TableColumn actionChainInterruptibleColumn;

    @FXML
    private TableColumn actionChainSchedulableColumn;

    @FXML
    private TableColumn actionChainLifetimeColumn;

    @FXML
    private TableColumn actionChainLastExtensionColumn;

    @FXML
    private TableColumn actionChainExecutionTimeColumn;

    @FXML
    private TableColumn actionChainTimestampColumn;

    @FXML
    private TableColumn actionChainValidColumn;

    @FXML
    private TableColumn actionChainRunningColumn;

    @FXML
    private TableColumn actionChainDoneColumn;

    @FXML
    private TableColumn actionChainExpiredColumn;

    @FXML
    private TableColumn actionChainUnitLabelColumn;

    private Observer<Remote<?>, State> connectionStateObserver;

    private Observer unitObserver;
    private UnitRemote<?> unit;
    private final ObservableList<UnitAllocationBean> unitAllocationData;
    private final ObservableList<ActionReferenceBean> actionChainData;

    public UnitAllocationPaneController() {
        this.unitIdProperty = new SimpleStringProperty();
        this.unitAllocationData = FXCollections.observableArrayList();
        this.actionChainData = FXCollections.observableArrayList();
        this.unitIdProperty.addListener((observable, oldValue, newValue) -> {
            try {
                setUnitId(newValue);
            } catch (CouldNotPerformException ex) {
                ExceptionPrinter.printHistory("Could not apply property change!", ex, LOGGER);
            }
        });
        this.unitObserver = (source, data) -> {
            update();
        };

        this.connectionStateObserver = (source, data) -> {
            actionTable.setDisable(data != State.CONNECTED);
            actionTable.setEditable(data != State.CONNECTED);
            actionChainTable.setDisable(data != State.CONNECTED);
            actionChainTable.setEditable(data != State.CONNECTED);
            if (data != State.CONNECTED) {
                actionTable.setStyle("-fx-border-color: #a00600; -fx-border-width: 3");
                actionChainTable.setStyle("-fx-border-color: #a00600; -fx-border-width: 3");
            } else {
                actionTable.setStyle("");
                actionChainTable.setStyle("");
            }
        };
    }

    @Override
    public void initContent() throws InitializationException {
        try {
            final Pair<Pane, UnitSelectionPaneController> unitSelectionPaneControllerPair = FXMLProcessor.loadFxmlPaneAndControllerPair(UnitSelectionPaneController.class);

            topPane.getChildren().add(unitSelectionPaneControllerPair.getKey());
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
            lastExtensionColumn.setCellValueFactory(new PropertyValueFactory<>("lastExtension"));
            validityTimeColumn.setCellValueFactory(new PropertyValueFactory<>("validityTime"));

            actionChainPositionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
            actionChainPriorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
            actionChainValidColumn.setCellValueFactory(new PropertyValueFactory<>("valid"));
            actionChainRunningColumn.setCellValueFactory(new PropertyValueFactory<>("running"));
            actionChainDoneColumn.setCellValueFactory(new PropertyValueFactory<>("done"));
            actionChainExpiredColumn.setCellValueFactory(new PropertyValueFactory<>("expired"));
            actionChainCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
            actionChainActionStateColumn.setCellValueFactory(new PropertyValueFactory<>("actionState"));
            actionChainActionIdColumn.setCellValueFactory(new PropertyValueFactory<>("actionId"));
            actionChainTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            actionChainLifetimeColumn.setCellValueFactory(new PropertyValueFactory<>("lifetime"));
            actionChainServiceStateColumn.setCellValueFactory(new PropertyValueFactory<>("serviceState"));
            actionChainUnitLabelColumn.setCellValueFactory(new PropertyValueFactory<>("unitLabel"));
            actionChainInitiatorColumn.setCellValueFactory(new PropertyValueFactory<>("initiator"));
            actionChainInterruptibleColumn.setCellValueFactory(new PropertyValueFactory<>("interruptible"));
            actionChainSchedulableColumn.setCellValueFactory(new PropertyValueFactory<>("schedulable"));
            actionChainExecutionTimeColumn.setCellValueFactory(new PropertyValueFactory<>("executionTime"));
            actionChainLastExtensionColumn.setCellValueFactory(new PropertyValueFactory<>("lastExtension"));

            unitSelectionPaneControllerPair.getValue().unitIdProperty().addListener((a, b, c) -> {
                unitIdProperty.set(c);
            });

            unitSelectionPaneControllerPair.getValue().setupServicePatternPass(ServicePattern.OPERATION);

            actionTable.setItems(unitAllocationData);
            actionChainTable.setItems(actionChainData);
            actionTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {

                // ignore null values
                if (newValue == null) {
                    return;
                }

                actionChainData.clear();

                try {
                    final ActionDescription actionDescription = ((UnitAllocationBean) newValue).getRemoteAction().getActionDescription();

                    int i = 0;
                    actionChainData.add(new ActionReferenceBean(i++, ActionDescriptionProcessor.generateActionReference(actionDescription)));

                    for (ActionReference actionReference : actionDescription.getActionCauseList()) {
                        actionChainData.add(new ActionReferenceBean(i++, actionReference));
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory("Could updated dynamic content!", ex, LOGGER);
                }

                actionChainTable.getSelectionModel().select(0);
            });


            GlobalScheduledExecutorService.scheduleAtFixedRate(() -> Platform.runLater(() -> {
                actionTable.refresh();
                actionChainTable.refresh();
            }), 1, 1, TimeUnit.SECONDS);

            actionTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
                @Override
                public void handle(KeyEvent keyEvent) {
                    if (keyEvent.getCode() != KeyCode.ESCAPE) {
                        return;
                    }
                    
                    try {
                        ((UnitAllocationBean) actionTable.getSelectionModel().getSelectedItem()).getRemoteAction().cancel().get(3, TimeUnit.SECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        ExceptionPrinter.printHistory(new CouldNotPerformException("Could not cancel action!", e), LOGGER);
                    }
                }
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

        unitAllocationData.clear();

        try {
            int i = 1;
            for (ActionDescription actionDescription : unit.getActionList()) {
                unitAllocationData.add(new UnitAllocationBean(i++, actionDescription));
            }
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could updated dynamic content!", ex, LOGGER);
        }
    }

    private void setUnitId(final String unitId) throws CouldNotPerformException {
        try {
            if (unit != null) {
                unit.removeDataObserver(unitObserver);
                unit.removeConnectionStateObserver(connectionStateObserver);
            }
            unit = Units.getUnit(unitId, false);
            unit.addDataObserver(unitObserver);
            unit.addConnectionStateObserver(connectionStateObserver);
            if (unit.isDataAvailable()) {
                update();
            }
            actionTable.getSelectionModel().select(0);
        } catch (NotAvailableException e) {
            throw new CouldNotPerformException("Could not setup Unit[" + unitId + "]");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

        public UnitAllocationBean(final int position, final ActionDescription actionDescription) throws InstantiationException {
            try {
                this.remoteAction = new RemoteAction(actionDescription);
                this.position = position;
                this.timestamp = dateFormat.format(new Date(TimestampJavaTimeTransform.transform(actionDescription.getTimestamp())));
                this.actionId = actionDescription.getActionId();
                this.description = MultiLanguageTextProcessor.getBestMatch(actionDescription.getDescription(), "");
                this.priority = actionDescription.getPriority().name();
                this.category = StringProcessor.transformCollectionToString(actionDescription.getCategoryList(), ", ");
                this.interruptible = actionDescription.getInterruptible();
                this.schedulable = actionDescription.getSchedulable();

                final ActionInitiator actionInitiator = ActionDescriptionProcessor.getInitialInitiator(actionDescription);

                String initiatorLabel;
                try {
                    initiatorLabel = ActionDescriptionProcessor.getInitiatorName(ActionDescriptionProcessor.getInitialInitiator(actionDescription));
                } catch (CouldNotPerformException e) {
                    initiatorLabel = (actionInitiator.getInitiatorId().equals(User.OTHER) ? User.OTHER : "");
                }

                this.initiator = initiatorLabel + " (" + actionInitiator.getInitiatorType().name() + " )";

                String tmpServiceState;
                try {
                    final Message serviceStateMessage = protoBufJSonProcessor.deserialize(actionDescription.getServiceStateDescription().getServiceState(), actionDescription.getServiceStateDescription().getServiceStateClassName());
                    tmpServiceState = StringProcessor.transformCollectionToString(Services.resolveStateValue(serviceStateMessage), ", ");
                } catch (CouldNotPerformException e) {
                    tmpServiceState = "";
                }
                this.serviceState = tmpServiceState;
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new InstantiationException(this, ex);
            }
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

                if (days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                        hours - TimeUnit.DAYS.toHours(days),
                        minutes - TimeUnit.HOURS.toMinutes(hours),
                        seconds - TimeUnit.MINUTES.toSeconds(minutes))
                        + (days > 0 ? " ( + " + days + " days)" : "");
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public String getLastExtension() {
            try {
                return dateFormat.format(new Date(remoteAction.getLastExtensionTime()));
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public String getLifetime() {
            try {
                final long lifetime = remoteAction.getLifetime();
                final long days = TimeUnit.MILLISECONDS.toDays(lifetime);
                final long hours = TimeUnit.MILLISECONDS.toHours(lifetime);
                final long minutes = TimeUnit.MILLISECONDS.toMinutes(lifetime);
                final long seconds = TimeUnit.MILLISECONDS.toSeconds(lifetime);

                if (days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                        hours - TimeUnit.DAYS.toHours(days),
                        minutes - TimeUnit.HOURS.toMinutes(hours),
                        seconds - TimeUnit.MINUTES.toSeconds(minutes))
                        + (days > 0 ? " ( + " + days + " days)" : "");
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public String getValidityTime() {
            try {
                final long executionTime = remoteAction.getValidityTime(TimeUnit.MILLISECONDS);
                final long days = TimeUnit.MILLISECONDS.toDays(executionTime);
                final long hours = TimeUnit.MILLISECONDS.toHours(executionTime);
                final long minutes = TimeUnit.MILLISECONDS.toMinutes(executionTime);
                final long seconds = TimeUnit.MILLISECONDS.toSeconds(executionTime);

                if (days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                        hours - TimeUnit.DAYS.toHours(days),
                        minutes - TimeUnit.HOURS.toMinutes(hours),
                        seconds - TimeUnit.MINUTES.toSeconds(minutes))
                        + (days > 0 ? " ( + " + days + " days)" : "");
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public boolean getInterruptible() {
            return interruptible;
        }

        public boolean getSchedulable() {
            return schedulable;
        }

        public RemoteAction getRemoteAction() {
            return remoteAction;
        }
    }

    public static class ActionReferenceBean {

        private final DateFormat dateFormat = DateFormat.getInstance();

        private final int position;
        private final String timestamp;
        private final String actionId;
        private final String serviceState;
        private final String initiator;
        private final String priority;
        private final String category;
        private final boolean interruptible;
        private final boolean schedulable;
        private final String unitLabel;

        private final RemoteAction remoteAction;
        final ActionReference actionReference;

        private final static ProtoBufJSonProcessor protoBufJSonProcessor = new ProtoBufJSonProcessor();

        public ActionReferenceBean(final int position, final ActionReference actionReference) throws InstantiationException {
            try {
                this.actionReference = actionReference;
                this.remoteAction = new RemoteAction(actionReference);
                this.position = position;
                this.timestamp = dateFormat.format(new Date(TimestampJavaTimeTransform.transform(actionReference.getTimestamp())));
                this.actionId = actionReference.getActionId();
                this.priority = actionReference.getPriority().name();
                this.category = StringProcessor.transformCollectionToString(actionReference.getCategoryList(), ", ");
                this.interruptible = actionReference.getInterruptible();
                this.schedulable = actionReference.getSchedulable();

                final ActionInitiator actionInitiator = actionReference.getActionInitiator();

                String initiatorLabel;
                try {
                    initiatorLabel = ActionDescriptionProcessor.getInitiatorName(actionReference.getActionInitiator());
                } catch (CouldNotPerformException e) {
                    initiatorLabel = (actionInitiator.getInitiatorId().equals(User.OTHER) ? User.OTHER : "");
                }

                this.initiator = initiatorLabel + " (" + actionInitiator.getInitiatorType().name() + " )";

                String tmpServiceState;
                try {
                    final Message serviceStateMessage = protoBufJSonProcessor.deserialize(actionReference.getServiceStateDescription().getServiceState(), actionReference.getServiceStateDescription().getServiceStateClassName());
                    tmpServiceState = StringProcessor.transformCollectionToString(Services.resolveStateValue(serviceStateMessage), ", ");
                } catch (CouldNotPerformException e) {
                    tmpServiceState = "";
                }
                this.serviceState = tmpServiceState;

                this.unitLabel = LabelProcessor.getBestMatch(Registries.getUnitRegistry().getUnitConfigById(actionReference.getServiceStateDescription().getUnitId()).getLabel(), "");
            } catch (CouldNotPerformException ex) {
                throw new InstantiationException(this, ex);
            }
        }

        public int getPosition() {
            return position;
        }

        public String getActionState() {

            switch (remoteAction.getActionState()) {
                case UNKNOWN:
                    return "";
                default:
                    return remoteAction.getActionState().name();
            }
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

                if (days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                        hours - TimeUnit.DAYS.toHours(days),
                        minutes - TimeUnit.HOURS.toMinutes(hours),
                        seconds - TimeUnit.MINUTES.toSeconds(minutes))
                        + (days > 0 ? " ( + " + days + " days)" : "");
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public String getLastExtension() {
            try {
                return dateFormat.format(new Date(remoteAction.getLastExtensionTime()));
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public String getLifetime() {
            try {
                final long lifetime = remoteAction.getLifetime();
                final long days = TimeUnit.MILLISECONDS.toDays(lifetime);
                final long hours = TimeUnit.MILLISECONDS.toHours(lifetime);
                final long minutes = TimeUnit.MILLISECONDS.toMinutes(lifetime);
                final long seconds = TimeUnit.MILLISECONDS.toSeconds(lifetime);

                if (days > 365) {
                    return "∞";
                }

                return String.format("%02d:%02d:%02d",
                        hours - TimeUnit.DAYS.toHours(days),
                        minutes - TimeUnit.HOURS.toMinutes(hours),
                        seconds - TimeUnit.MINUTES.toSeconds(minutes))
                        + (days > 0 ? " ( + " + days + " days)" : "");
            } catch (NotAvailableException e) {
                return "";
            }
        }

        public boolean getInterruptible() {
            return interruptible;
        }

        public boolean getSchedulable() {
            return schedulable;
        }

        public RemoteAction getRemoteAction() {
            return remoteAction;
        }

        public String getUnitLabel() {
            return unitLabel;
        }
    }
}
