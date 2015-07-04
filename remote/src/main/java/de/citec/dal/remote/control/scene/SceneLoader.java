/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.scene;

import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.ExceptionPrinter;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.pattern.Observable;
import de.citec.jul.pattern.Observer;
import de.citec.scm.remote.SceneRegistryRemote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import rst.homeautomation.control.scene.SceneConfigType.SceneConfig;
import rst.homeautomation.control.scene.SceneRegistryType;

/**
 *
 * @author mpohling
 */
public class SceneLoader {

    private final HashMap<String, SceneInterface> agentMap;
    private final SceneRegistryRemote agentRegistryRemote;


    public SceneLoader() throws InstantiationException, InterruptedException {
        try {
            agentMap = new HashMap<>();

            agentRegistryRemote = new SceneRegistryRemote();
            agentRegistryRemote.addObserver(new Observer<SceneRegistryType.SceneRegistry>() {

                @Override
                public void update(Observable<SceneRegistryType.SceneRegistry> source, SceneRegistryType.SceneRegistry data) throws Exception {
                    updateScenes(data);
                }
            });
            agentRegistryRemote.init();
            agentRegistryRemote.activate();
            System.out.println("waiting for agents...");

        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    private void updateScenes(final SceneRegistryType.SceneRegistry data) throws InterruptedException {

        // add new agents
        for (SceneConfig config : data.getSceneConfigList()) {

            if (!agentMap.containsKey(config.getId())) {
                try {
                    agentMap.put(config.getId(), createScene(config));
                    
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(null, ex);
                }
            }
        }

        boolean found;
        // remove outdated agents
        for (SceneInterface scene : new ArrayList<>(agentMap.values())) {
            found = false;
            for (SceneConfig config : data.getSceneConfigList()) {
                try {
                    if (scene.getConfig().getId().equals(config.getId())) {
                        found = true;
                        break;
                    }
                } catch (NotAvailableException ex) {
                    continue;
                }
            }

            if (!found) {
                try {
                    agentMap.remove(scene.getConfig().getId()).deactivate();
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(null, ex);
                }
            }
        }
    }

    public SceneInterface createScene(final SceneConfig config) throws CouldNotPerformException {
        SceneInterface scene = new Scene(config);
        try {
            scene.activate();
        } catch (InterruptedException ex) {
            ExceptionPrinter.printHistory(null, ex);
        }
        return scene;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws de.citec.jul.exception.InstantiationException, InterruptedException, CouldNotPerformException, ExecutionException {
        new SceneLoader();
    }
}
