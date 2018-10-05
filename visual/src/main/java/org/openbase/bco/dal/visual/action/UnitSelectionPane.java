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

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.visual.javafx.control.AbstractFXController;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

public class UnitSelectionPane extends AbstractFXController {

    @FXML
    private ComboBox locationComboBox;

    @FXML
    private ComboBox<UnitType> unitTypeComboBox;

    @FXML
    private ComboBox serviceTypeComboBox;

    @Override
    public void initContent() throws InitializationException {

    }

    @Override
    public void updateDynamicContent() {
        if (!Registries.isDataAvailable()) {
            locationComboBox.setDisable(true);
            unitTypeComboBox.setDisable(true);
            serviceTypeComboBox.setDisable(true);
        }

        try {
            locationComboBox.getItems().addAll(Registries.getUnitRegistry().getUnitConfigs(UnitType.LOCATION));
            unitTypeComboBox.getItems().addAll(UnitType.values());
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory("Could not update dynamic content!", ex, System.out);
        }
    }
}
