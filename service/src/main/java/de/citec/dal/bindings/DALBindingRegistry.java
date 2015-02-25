/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings;

import de.citec.dal.registry.AbstractRegistry;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author mpohling
 */
public class DALBindingRegistry extends AbstractRegistry<Class<? extends Binding>, Binding> {

    public <BC extends Binding> BC getBinding(Class<BC> key) throws NotAvailableException {
        return (BC) super.get(key);
    }

    public void register(Class<? extends Binding> bindingClazz) throws CouldNotPerformException {
        try {
            if (contrains(bindingClazz)) {
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
