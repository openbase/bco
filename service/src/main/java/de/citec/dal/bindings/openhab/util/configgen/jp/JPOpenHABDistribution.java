/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen.jp;

import de.citec.jps.core.JPService;
import de.citec.jps.exception.JPNotAvailableException;
import de.citec.jps.preset.AbstractJPDirectory;
import de.citec.jps.preset.JPPrefix;
import de.citec.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class JPOpenHABDistribution extends AbstractJPDirectory {
    private static final String[] COMMAND_IDENTIFIERS = {"--openhab-dist"};

    public JPOpenHABDistribution() {
        super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.Off);
    }

    @Override
    protected File getPropertyDefaultValue() throws JPNotAvailableException {
        return new File(JPService.getProperty(JPPrefix.class).getValue(), "share/openhab/distribution");
    }

    @Override
    public String getDescription() {
        return "Defines the openhab distribution directory.";
    }

}
