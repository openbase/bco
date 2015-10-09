package de.citec.dal.example;

import de.citec.dal.remote.unit.AmbientLightRemote;
import de.citec.jps.core.JPService;
import de.citec.jps.preset.JPDebugMode;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.printer.ExceptionPrinter;
import de.citec.jul.exception.printer.LogLevel;
import java.awt.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author * @author <a href="mailto:DivineThreepwood@gmail.com">Divine Threepwood</a>
 */
public class ControlAmbientLightViaRemoteLib {

    private static final Logger logger = LoggerFactory.getLogger(ControlAmbientLightViaRemoteLib.class);

    public void notifyAlarm() throws CouldNotPerformException, InterruptedException {
        try {
            AmbientLightRemote testLight = new AmbientLightRemote();
            testLight.init(new Scope("/home/control/ambientlight/testunit_0/"));
            testLight.activate();

            int delay = 500;
            int rounds = 100;

            for (int i = 0; i < rounds; i++) {
                try {
                    testLight.setColor(Color.BLUE);
                    Thread.sleep(delay);
                    testLight.setColor(Color.RED);
                    Thread.sleep(delay);
                } catch (CouldNotPerformException | InterruptedException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not change color!", ex), logger, LogLevel.ERROR);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not notify alarm!", ex);
        }
    }

    /**
     * 
     * @param args
     * @throws de.citec.jul.exception.InstantiationException
     * @throws CouldNotPerformException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws de.citec.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        /* Setup CLParser */
        JPService.setApplicationName(ControlAmbientLightViaRemoteLib.class.getSimpleName().toLowerCase());
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        JPService.registerProperty(JPDebugMode.class, true);
        JPService.parseAndExitOnError(args);

        new ControlAmbientLightViaRemoteLib().notifyAlarm();
    }
}
