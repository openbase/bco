package org.openbase.bco.dal.visual.action;

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
