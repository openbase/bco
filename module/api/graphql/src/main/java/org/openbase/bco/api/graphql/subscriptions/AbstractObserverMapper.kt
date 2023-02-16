package org.openbase.bco.api.graphql.subscriptions

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import org.openbase.jul.exception.CouldNotPerformException
import org.openbase.jul.pattern.Observer
import java.util.function.Consumer

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
 */ abstract class AbstractObserverMapper<S, T, E> : Observer<S, T> {
    private var emitter: ObservableEmitter<E>? = null
    @Throws(Exception::class)
    override fun update(source: S, target: T) {
        if (emitter == null) {
            return
        }
        emitter!!.onNext(mapData(source, target))
    }

    fun setEmitter(emitter: ObservableEmitter<E>?) {
        this.emitter = emitter
    }

    @Throws(Exception::class)
    abstract fun mapData(source: S, data: T): E
    @Throws(CouldNotPerformException::class)
    open fun doAfterRemoveObserver() {
    }

    @Throws(CouldNotPerformException::class, InterruptedException::class)
    open fun doAfterAddObserver() {
    }

    companion object {
        fun <S, T, E> createObservable(
            addObserver: Consumer<Observer<S, T>>,
            removeObserver: Consumer<Observer<S, T>>,
            obs: AbstractObserverMapper<S, T, E>
        ): Observable<E> {
            var observable = Observable.create { emitter: ObservableEmitter<E>? ->
                obs.setEmitter(emitter)
                addObserver.accept(obs)
                obs.doAfterAddObserver()
            }
            observable = observable.doOnDispose {
                removeObserver.accept(obs)
                obs.doAfterRemoveObserver()
            }
            return observable
        }
    }
}
