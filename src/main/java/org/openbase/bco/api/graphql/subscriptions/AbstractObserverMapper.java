package org.openbase.bco.api.graphql.subscriptions;

/*-
 * #%L
 * BCO GraphQL API
 * %%
 * Copyright (C) 2020 openbase.org
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

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.pattern.Observer;

import java.util.function.Consumer;

public abstract class AbstractObserverMapper<S, T, E> implements Observer<S, T> {

    private ObservableEmitter<E> emitter = null;

    @Override
    public void update(final S source, T target) throws Exception {
        System.out.println("Observer mapper received update...");
        if (emitter == null) {
            return;
        }

        emitter.onNext(mapData(target));
    }

    public void setEmitter(final ObservableEmitter<E> emitter) {
        this.emitter = emitter;
    }

    public abstract E mapData(final T data) throws Exception;

    public void doAfterRemoveObserver() throws CouldNotPerformException {
    }

    public void doAfterAddObserver() throws CouldNotPerformException, InterruptedException {
    }

    public static <S, T, E> Observable<E> createObservable(Consumer<Observer<S, T>> addObserver, Consumer<Observer<S, T>> removeObserver, AbstractObserverMapper<S, T, E> obs) {
        System.out.println("Create observable...");
        Observable<E> observable = Observable.create(emitter -> {
            System.out.println("Create an add emitter");
            obs.setEmitter(emitter);
            addObserver.accept(obs);
            obs.doAfterAddObserver();
        });

        observable = observable.doOnDispose(() -> {
            removeObserver.accept(obs);
            obs.doAfterRemoveObserver();
        });

        return observable;
    }
}
