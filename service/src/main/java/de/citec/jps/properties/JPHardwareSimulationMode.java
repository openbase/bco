/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jps.properties;

import de.citec.jps.core.AbstractJavaProperty;
import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPNotAvailableException;
import de.citec.jps.exception.JPValidationException;
import de.citec.jps.preset.AbstractJPBoolean;
import de.citec.jps.preset.JPTestMode;

/**
 *
 * @author Divine Threepwood
 */
public class JPHardwareSimulationMode extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--simulate-hardware", "-s"};

    public JPHardwareSimulationMode() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Boolean getPropertyDefaultValue() throws JPNotAvailableException {
        return JPService.getProperty(JPTestMode.class).getValue();
    }

    @Override
    public void validate() throws JPValidationException {
        super.validate();
        if (!getValueType().equals((AbstractJavaProperty.ValueType.PropertyDefault))) {
            logger.warn("Started in hardware simulation mode!!");
        }
    }

    @Override
    public String getDescription() {
        return "Simulates the hardware components.";
    }
}
