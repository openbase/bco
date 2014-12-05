/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author divine
 * @param <T>
 */
public class Observable<T> {

    private static final Object LOCK = new Object();

    private final List<Observer<T>> observers;

    public Observable() {
        this.observers = new ArrayList<>();
    }

    public void addObserver(Observer<T> observer) {
        synchronized (LOCK) {
            if (!observers.contains(observer)) {
                observers.add(observer);
            }
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
        List<Exception> exceptionStack = null;

        synchronized (LOCK) {
            for (Observer<T> observer : observers) {
                try {
                    observer.update(this, arg);
                } catch (Exception ex) {
                    if(exceptionStack == null) {
                        exceptionStack = new ArrayList<>();
                    }
                    exceptionStack.add(ex);
                }
            }
        }

        if (exceptionStack != null) {
            throw new MultiException("Could not notify Data["+arg+"] to all observer!", exceptionStack);
        }
    }
}
