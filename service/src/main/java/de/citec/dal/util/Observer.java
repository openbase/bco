/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

/**
 *
 * @author divine
 */
public interface Observer<T> {
    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param   o     the observable object.
     * @param   arg   an argument passed to the <code>notifyObservers</code>
     *                 method.
     */
    public void update(Observable<T> source, T data );
}