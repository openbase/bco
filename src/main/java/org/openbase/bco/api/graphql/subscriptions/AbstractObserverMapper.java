package org.openbase.bco.api.graphql.subscriptions;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import org.openbase.jul.pattern.Observer;

import java.util.function.Function;

public abstract class AbstractObserverMapper<S, T, E> implements Observer<S, T> {

    private ObservableEmitter<E> emitter = null;

    @Override
    public void update(final S source, T target) throws Exception {
        if (emitter == null) {
            return;
        }

        emitter.onNext(mapData(target));
    }

    public void setEmitter(final ObservableEmitter<E> emitter) {
        this.emitter = emitter;
    }

    public abstract E mapData(final T data) throws Exception;

    public static <A, B, C> Observable<A> createObservable(Function<Observer<B, C>, Void> addObserver, Function<Observer<B, C>, Void> removeObserver, AbstractObserverMapper<B, C, A> obs) {
        Observable<A> observable = Observable.create(emitter -> {
            obs.setEmitter(emitter);
            addObserver.apply(obs);
        });

        observable = observable.doOnDispose(() -> removeObserver.apply(obs));

        return observable;
    }
}
