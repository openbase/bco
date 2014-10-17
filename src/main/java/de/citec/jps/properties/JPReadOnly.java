/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jps.properties;

import de.citec.jps.core.AbstractJavaProperty.ValueType;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.AbstractJPBoolean;

/**
 *
 * @author mpohling
 */
public class JPReadOnly extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--read-only", "-r"};

    public JPReadOnly() {
        super(COMMAND_IDENTIFIERS);
    }
    
    @Override
    protected Boolean getPropertyDefaultValue() {
        return !JPService.getAttribute(JPDeviceManagerConfigPath.class).getValue().canWrite();
    }

    @Override
    public void validate() throws Exception {
        super.validate();
        if (!getValueType().equals((ValueType.PropertyDefault))) {
            logger.warn("Started in read only mode!");
        }
    }

    @Override
    public String getDescription() {
        return "Starts the device manager in a read only mode!";
    }
}
