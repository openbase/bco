/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.jp;

import de.citec.jps.core.AbstractJavaProperty.ValueType;
import de.citec.jps.preset.AbstractJPBoolean;

/**
 *
 * @author mpohling
 */
public class JPInitializeDBFlag extends AbstractJPBoolean {

    public final static String[] COMMAND_IDENTIFIERS = {"--init"};

    public JPInitializeDBFlag() {
        super(COMMAND_IDENTIFIERS);
    }
    
    @Override
    protected Boolean getPropertyDefaultValue() {
        return false;
    }

    @Override
    public void validate() throws Exception {
        super.validate();
        if (getValueType().equals((ValueType.CommandLine))) {
            logger.warn("WARNING: YOU WILL OVERWRITE THE CURRENT DATABASE!!!");
            logger.warn("=== Press enter to contine ===");
            if(!(System.in.read() == 'y')) {
                System.exit(1);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Initialize a new instance of the interal database.";
    }
}
