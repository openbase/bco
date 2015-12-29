/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.storage.registry.Registry;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 *
 * @author mpohling
 */
public class DALBindingRegistry extends Registry<Class<? extends Binding>, Binding> {

    public DALBindingRegistry() throws org.dc.jul.exception.InstantiationException {
    }

    public DALBindingRegistry(HashMap<Class<? extends Binding>, Binding> entryMap) throws org.dc.jul.exception.InstantiationException {
        super(entryMap);
    }

    public <BC extends Binding> BC getBinding(Class<BC> key) throws CouldNotPerformException {
        return (BC) super.get(key);
    }

    public void register(Class<? extends Binding> bindingClazz) throws CouldNotPerformException {
        try {
            if (contains(bindingClazz)) {
                throw new CouldNotPerformException("Binding instance already registered!");
            }
            try {
                register(bindingClazz.getConstructor().newInstance());
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                throw new CouldNotPerformException("Could not create binding instance out of " + bindingClazz + "!", ex);
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not register " + bindingClazz + "! ", ex);
        }
    }
}
