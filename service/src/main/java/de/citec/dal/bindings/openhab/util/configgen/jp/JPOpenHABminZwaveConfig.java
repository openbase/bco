/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.util.configgen.jp;

import org.dc.jps.core.JPService;
import org.dc.jps.exception.JPNotAvailableException;
import org.dc.jps.preset.AbstractJPDirectory;
import org.dc.jps.tools.FileHandler;
import java.io.File;

/**
 *
 * @author mpohling
 */
public class JPOpenHABminZwaveConfig extends AbstractJPDirectory {
    private static final String[] COMMAND_IDENTIFIERS = {"--hambin-zwave"};

    public JPOpenHABminZwaveConfig() {
        super(COMMAND_IDENTIFIERS, FileHandler.ExistenceHandling.Must, FileHandler.AutoMode.On);
    }

    @Override
    protected File getPropertyDefaultValue() throws JPNotAvailableException {
        return new File(JPService.getProperty(JPOpenHABDistribution.class).getValue(), "etc/zwave");
    }

    @Override
    public String getDescription() {
        return "Define the habmin zwave config folder location.";
    }
}
