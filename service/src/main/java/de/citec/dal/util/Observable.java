/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author divine
 * @param <T>
 */
public class Observable<T> {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Object LOCK = new Object();
    private final List<Observer<T>> observers;

    public Observable() {
        this.observers = new ArrayList<>();
    }

    public void addObserver(Observer<T> observer) {
        synchronized (LOCK) {
            if (observers.contains(observer)) {
				logger.warn("Skip observer registration. Observer["+observer+"] is already registered!");
                return;
            }

			observers.add(observer);
        }
    }

    public void removeObserver(Observer<T> observer) {
        synchronized (LOCK) {
            observers.remove(observer);
        }
    }

    public void shutdown() {
        synchronized (LOCK) {
            observers.clear();
        }
    }

    public void notifyObservers(T arg) throws MultiException {
        Map<Object, Exception> exceptionMap = null;

        synchronized (LOCK) {
            for (Observer<T> observer : observers) {
                try {
                    observer.update(this, arg);
                } catch (Exception ex) {
                    if(exceptionMap == null) {
                        exceptionMap = new HashMap<>();
                    }
					exceptionMap.put(observer, ex);
                }
            }
        }

        if (exceptionMap != null) {
            throw new MultiException("Could not notify Data["+arg+"] to all observer!", exceptionMap);
        }
    }
}
