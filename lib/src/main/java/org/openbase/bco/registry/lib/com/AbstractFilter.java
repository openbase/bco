package org.openbase.bco.registry.lib.com;

/*-
 * #%L
 * BCO Registry Lib
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import java.util.List;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observer;

/**
 * Filter which decides for a list of objects which to keep and which to filter out.
 * 
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.de">Tamino Huxohl</a>
 * @param <T> the type of object on which the filter works
 */
public abstract class AbstractFilter<T> {

    /**
     * Filter object from the list for which the verification fails.
     * 
     * @param list the list which is filtered
     * @return a filtered list
     * @throws CouldNotPerformException if an error occurs while filtering 
     */
    public List<T> filter(List<T> list) throws CouldNotPerformException {
        beforeFilter();
        for (int i = 0; i < list.size(); i++) {
            if (!verify(list.get(i))) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

    /**
     * This method is called once before the filtering is applied.
     * 
     * @throws CouldNotPerformException if an error occurs.
     */
    public abstract void beforeFilter() throws CouldNotPerformException;

    /**
     * Verifies an object of type t.
     * 
     * @param type the object which is verified
     * @return true if it should be kept and else false
     * @throws CouldNotPerformException if the verification fails
     */
    public abstract boolean verify(T type) throws CouldNotPerformException;
    
    /**
     * A filter can depend on some other processes. To be notified
     * when the filter will change an observer can be registered.
     * 
     * @param observer An observer which is notified when the filter changes. 
     */
    public abstract void addObserver(Observer observer);
    
    /**
     * Remove an observer which is added by addObserver.
     * 
     * @param observer The observer to be removed.
     */
    public abstract void removeObserver(Observer observer);

}
