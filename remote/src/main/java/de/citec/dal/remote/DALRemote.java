package de.citec.dal.remote;

import de.citec.dal.remote.jp.JPRemoteMethod;
import de.citec.dal.remote.jp.JPRemoteMethodParameters;
import de.citec.dal.remote.jp.JPRemoteService;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.extension.rsb.scope.jp.JPScope;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author mpohling
 */
public class DALRemote {

    private static final Logger logger = LoggerFactory.getLogger(DALRemote.class);

    public static final String APP_NAME = DALRemote.class.getSimpleName();

    public DALRemote() throws InitializationException {
        try {
            RSBRemoteService remote;
            try {
                remote = JPService.getProperty(JPRemoteService.class).getValue().newInstance();
            } catch (InstantiationException ex) {
                throw new CouldNotPerformException("Could not build remote instance!", ex);
            } catch (IllegalAccessException ex) {
                throw new CouldNotPerformException("Could not access remote!", ex);
            }
            remote.init(JPService.getProperty(JPScope.class).getValue());
            remote.activate();
            Method remoteMethod = JPService.getProperty(JPRemoteMethod.class).getValue();

            ArrayList parameterList = new ArrayList(remoteMethod.getParameterTypes().length);
            for (Class parameterClass : remoteMethod.getParameterTypes()) {
                Method parameterProcessorMethod;
                try {
                    parameterProcessorMethod = parameterClass.getMethod("valueOf", String.class);
                } catch (Exception ex) {
                    throw new CouldNotPerformException("Could not find parameter processor method!", ex);
                }

                try {
                    List<String> parameterStringList = new ArrayList(JPService.getProperty(JPRemoteMethodParameters.class).getValue());
                    parameterList.add(parameterProcessorMethod.invoke(null, parameterStringList.remove(0)));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NullPointerException ex) {
                    throw new CouldNotPerformException("Could transform parameter value into parameter!", ex);
                }
            }
            try {
                remoteMethod.invoke(remote, parameterList.toArray());
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new CouldNotPerformException("Could not invoke remote method!", ex);
            }
//            Thread.sleep(5000);
            remote.shutdown();
        } catch (Exception ex) {
            throw new InitializationException(this, ex);
        }
    }

    public static void main(String args[]) throws Throwable {
        logger.info("Start " + APP_NAME + "...");

        /* Setup JPService */
        JPService.setApplicationName(APP_NAME);

        JPService.registerProperty(JPDebugMode.class);
        JPService.registerProperty(JPScope.class);
        JPService.registerProperty(JPRemoteService.class);
        JPService.registerProperty(JPRemoteMethod.class);
        JPService.registerProperty(JPRemoteMethodParameters.class);

        JPService.parseAndExitOnError(args);

        try {
            new DALRemote();
        } catch (InitializationException ex) {
            throw ExceptionPrinter.printHistoryAndReturnThrowable(logger, ex);
        }
        logger.info(APP_NAME + " successfully started.");
    }
}
