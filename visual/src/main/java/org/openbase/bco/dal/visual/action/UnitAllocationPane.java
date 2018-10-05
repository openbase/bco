package org.openbase.bco.dal.visual.action;

/*-
 * #%L
 * BCO DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import org.openbase.bco.dal.lib.layer.unit.Unit;
import org.openbase.bco.dal.remote.layer.unit.Units;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import org.openbase.jul.visual.javafx.fxml.FXMLProcessor;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.state.ActionStateType.ActionState.State;

public class UnitAllocationPane extends AbstractFXController {

    @FXML
    private BorderPane topPane;

    @FXML
    private TableView actionTable;

    private Observer unitObserver;
    private Unit<?> unit;

    public UnitAllocationPane() {
        this.unitObserver = (source, data) -> {
            updateDynamicContent();
        };
    }

    @Override
    public void initContent() throws InitializationException {
        try {
            topPane.setCenter(FXMLProcessor.loadFxmlPane("org/openbase/bco/dal/visual/action/UnitSelectionPane.fxml", UnitSelectionPane.class));
            TableColumn positionCol = new TableColumn("Pos");
            positionCol.setMinWidth(100);
            positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));

            TableColumn actionStateCol = new TableColumn("Action State");
            actionStateCol.setMinWidth(100);
            actionStateCol.setCellValueFactory(new PropertyValueFactory<>("actionState"));

            TableColumn actionIdCol = new TableColumn("Action Id");
            actionIdCol.setMinWidth(100);
            actionIdCol.setCellValueFactory(new PropertyValueFactory<>("actionId"));

            TableColumn timestampCol = new TableColumn("Timestamp");
            timestampCol.setMinWidth(100);
            timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

            actionTable.getColumns().addAll(positionCol, positionCol, actionIdCol, timestampCol);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public void updateDynamicContent() {

        if(unit == null) {
            return;
        }

        ObservableList<UnitAllocationBean> data = FXCollections.observableArrayList();

        try {
            int i = 1;
            for (ActionDescription actionDescription : unit.getActionList()) {
                data.add(new UnitAllocationBean(i++, actionDescription));
            }
        } catch (NotAvailableException e) {
            e.printStackTrace();
        }
        actionTable.setItems(data);
    }

    public void setUnitId(final String unitId) {
        try {
            if (unit != null) {
                unit.removeDataObserver(unitObserver);
            }

            unit = Units.getUnit(unitId, false);
            unit.addDataObserver(unitObserver);
            if (unit.isDataAvailable()) {
                updateDynamicContent();
            }
        } catch (NotAvailableException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class UnitAllocationBean {

        private final int position;
        private final State actionState;
        private final long timestamp;
        private final String actionId;

        public UnitAllocationBean(final int position, final ActionDescription actionDescriptions) {
            this.position = position;
            this.actionState = actionDescriptions.getActionState().getValue();
            this.timestamp = actionDescriptions.getTimestamp().getTime();
            this.actionId = actionDescriptions.getId();
        }

        public int getPosition() {
            return position;
        }

        public State getActionState() {
            return actionState;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getActionId() {
            return actionId;
        }
    }
}
