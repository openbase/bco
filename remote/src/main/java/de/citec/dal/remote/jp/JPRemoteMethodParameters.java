/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.jp;

import de.citec.jps.core.AbstractJavaProperty;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mpohling
 */
public class JPRemoteMethodParameters extends AbstractJavaProperty<List<String>> {

    public final static String[] COMMAND_IDENTIFIERS = {"-p", "--parameter"};

    public JPRemoteMethodParameters() {
        super(COMMAND_IDENTIFIERS);
    }

    @Override
    public String getDescription() {
        return "Specifies the remote method parameters for the message invocation.";
    }

    @Override
    protected List<String> getPropertyDefaultValue() {
        return new ArrayList<String>();
    }

    @Override
    protected String[] generateArgumentIdentifiers() {
        String[] args = new String[1];
        args[0] = "0..N";
        return args;
    }

    @Override
    protected List<String> parse(List<String> arguments) throws Exception {
        return arguments;
    }
}
