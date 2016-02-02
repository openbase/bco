/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.binding;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.storage.registry.RegistryImpl;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 *
 * @author mpohling
 */
public class DALBindingRegistryImpl extends RegistryImpl<Class<? extends Binding>, Binding> {

    public DALBindingRegistryImpl() throws org.dc.jul.exception.InstantiationException {
    }

    public DALBindingRegistryImpl(HashMap<Class<? extends Binding>, Binding> entryMap) throws org.dc.jul.exception.InstantiationException {
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
