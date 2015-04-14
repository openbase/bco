/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.jp;

import de.citec.jps.preset.AbstractJPClass;
import de.citec.jul.rsb.com.RSBRemoteService;

/**
 *
 * @author mpohling
 */
public class JPRemoteService extends AbstractJPClass<RSBRemoteService> {

    public final static String[] COMMAND_IDENTIFIERS = {"-r", "--remote"};

    public JPRemoteService() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public String getDescription() {
        return "Specifies the remote sevice to use for controlling.";
    }

    @Override
    protected Class<RSBRemoteService> getPropertyDefaultValue() {
        return null;
    }
}
