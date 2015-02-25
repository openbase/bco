/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.csra.dm;

import de.citec.csra.dm.registry.DeviceRegistryImpl;
import de.citec.jps.core.JPService;
import de.citec.jp.JPReadOnly;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.rsb.RSBInformerInterface;
import de.citec.jul.rsb.jp.JPScope;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author mpohling
 */
public class DeviceManager {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceManager.class);
    
    private final DeviceRegistryImpl deviceRegistry;

    public DeviceManager() throws InitializationException {
        try {
        this.deviceRegistry = new DeviceRegistryImpl();
        this.deviceRegistry.init(RSBInformerInterface.InformerType.Single);
        this.deviceRegistry.activate();
        } catch(CouldNotPerformException ex ) {
            throw new InitializationException(this, ex);
        }        
    }
    
    public static void main(String args[]) throws Throwable {

        try {
            /* Setup CLParser */
            JPService.setApplicationName("DeviceManager");
//
            JPService.registerProperty(JPScope.class, new Scope("/devicemanager/registry"));
            JPService.registerProperty(JPReadOnly.class);
//        JPService.registerProperty(JPShowGUI.class, true);
            JPService.parseAndExitOnError(args);
            
//        if (JPService.getAttribute(JPShowGUI.class).getValue()) {
//            DevieManagerGUI.main(args);
//        }
//        getInstance();
            
            new DeviceManager();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistory(logger, ex);
        }
    }
}
