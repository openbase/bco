package org.openbase.bco.dal.visual.action;

import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import org.openbase.jul.visual.javafx.fxml.FXMLProcessor;
import org.openbase.jul.visual.javafx.launch.AbstractFXMLApplication;

public class BCOActions extends AbstractFXMLApplication {

    public BCOActions() {
        super();
    }

    @Override
    protected String getDefaultCSS() {
        return "/styles/main-style.css";
    }

    @Override
    protected String getDefaultFXML() {
        return "org/openbase/bco/dal/visual/action/UnitAllocationPane.fxml";
    }

    @Override
    protected void registerProperties() {
    }
}
