/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jps.properties;

import org.dc.jps.core.AbstractJavaProperty;
import org.dc.jps.exception.JPValidationException;
import org.dc.jps.preset.AbstractJPBoolean;

/**
 *
 * @author Divine Threepwood
 */
public class JPUnitLabelList extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--units"};

    public JPUnitLabelList() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    protected Boolean getPropertyDefaultValue() {
        return false;
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

