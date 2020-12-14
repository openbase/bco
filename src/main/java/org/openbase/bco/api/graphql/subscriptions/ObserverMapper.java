package org.openbase.bco.api.graphql.subscriptions;

import io.reactivex.ObservableEmitter;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.type.domotic.unit.dal.ColorableLightDataType;

public class ObserverMapper implements Observer<DataProvider<ColorableLightDataType.ColorableLightData>, ColorableLightDataType.ColorableLightData> {

    private ObservableEmitter<String> emitter = null;

    @Override
    public void update(DataProvider<ColorableLightDataType.ColorableLightData> source, ColorableLightDataType.ColorableLightData data) throws Exception {
        if (emitter == null) {
            return;
        }

        emitter.onNext(data.getPowerState().getValue().name());
    }

    public void setEmitter(ObservableEmitter<String> emitter) {
        this.emitter = emitter;
    }
}
