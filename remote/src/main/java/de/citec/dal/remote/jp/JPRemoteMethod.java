/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.jp;

import de.citec.jps.preset.AbstractJPMethod;
import de.citec.jul.rsb.RSBRemoteService;
import java.lang.reflect.Method;

/**
 *
 * @author mpohling
 */
public class JPRemoteMethod extends AbstractJPMethod<RSBRemoteService> {

    public final static String[] COMMAND_IDENTIFIERS = {"-m", "--remoteMethod"};

    public JPRemoteMethod() {
        super(COMMAND_IDENTIFIERS, JPRemoteService.class);
    }

    @Override
    public String getDescription() {
        return "Specifies the remote sevice methode to call.";
    }
    
    @Override
    protected Method getPropertyDefaultValue() {
        return null;
    }
}
