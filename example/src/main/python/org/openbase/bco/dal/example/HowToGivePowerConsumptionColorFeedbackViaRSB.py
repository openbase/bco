#!/usr/bin/env python2

import logging
import rsb
import rst
import rstsandbox
import rstexperimental
from rst.domotic.unit.dal.PowerConsumptionSensorData_pb2 import PowerConsumptionSensorData
from rst.domotic.unit.location.LocationData_pb2 import LocationData
from rst.domotic.unit.UnitConfig_pb2 import UnitConfig
from rst.vision.HSBColor_pb2 import HSBColor
from rst.domotic.state.PowerState_pb2 import PowerState
from rsb.converter import ProtocolBufferConverter, registerGlobalConverter
import time

class HowToGivePowerConsumptionColorFeedbackViaRSB(object):
    def __init__(self):
        logging.basicConfig()
        registerGlobalConverter(ProtocolBufferConverter(messageClass=UnitConfig))
        registerGlobalConverter(ProtocolBufferConverter(messageClass=PowerConsumptionSensorData))
        registerGlobalConverter(ProtocolBufferConverter(messageClass=HSBColor))
        registerGlobalConverter(ProtocolBufferConverter(messageClass=PowerState))
        registerGlobalConverter(ProtocolBufferConverter(messageClass=LocationData))
        rsb.__defaultParticipantConfig = rsb.ParticipantConfig.fromDefaultSources()

        self.hue1 = 240
        self.hue2 = 0
        self.lasthue = None

        self.unit_registry_scope = "/registry/unit/ctrl"

        self.location_id = "f0a71f71-1463-41e3-9c9a-25a02a536001"
        self.light_id = "8d310f30-d60a-4627-8884-373c5e2dcbdd"

        print("Waiting for unit registry...")
        with rsb.createRemoteServer(self.unit_registry_scope) as unit_registry:
            self.location_scope = self.transform_scope(unit_registry.getUnitConfigById(self.location_id).scope)
            print("Found kitchen scope: " + str(self.location_scope))
            self.light_scope = self.transform_scope(unit_registry.getUnitConfigById(self.light_id).scope)
            print("Found light scope: " + str(self.light_scope))

    @classmethod
    def transform_scope(self, scope):
        """
        build rsb scope out of the given rst scope.
        """
        return "/" + "/".join(scope.component)


    def run(self):
        print("Listening for power consumption events...")
        def power_update(event):
            #print("Received event: %s" % event)
            try:
                consumption = event.getData()
                if consumption == None:
                    print 'Null data received. Indicates remote controller shutdown.'
                    return
                consumption = consumption.power_consumption_state.consumption
                print 'consumption is', consumption, 'W (at time', str(time.time()) + ')'
                self.update_light_color(consumption)

            except Exception as e:
                +3

                print 'received illegal event (' + str(e) + ')'
                traceback.print_exc()

        with rsb.createListener(self.location_scope + "/status") as listener:
            listener.addHandler(power_update)
            while True:
                time.sleep(1)

    def update_light_color(self, current_consumption=0):
        with rsb.createRemoteServer(self.light_scope + "/ctrl") as server:
            # compute color value
            consumption_color = HSBColor()
            consumption_color.saturation = 100
            consumption_color.brightness = 100

            lowerhue, higherhue = sorted([self.hue1, self.hue2])
            doeswrap = (lowerhue - higherhue + 360) < (higherhue - lowerhue)

            if doeswrap:
                lowerhue, higherhue = (higherhue, lowerhue + 360)

            consumption_color.hue = self._linmap(current_consumption, [0, 1000], [lowerhue, higherhue], crop=True) % 360

            if consumption_color.hue != self.lasthue:
                print 'setting hue %i for power consumption %f W' % (consumption_color.hue, current_consumption)
                server.setColor.async(consumption_color)
                self.lasthue = consumption_color.hue

    def _linmap(self, inputvalue, inputrange, outputrange, crop=False):
        """
        Linearly map inputvalue from the inputrange to the outputrange. Convert type (usually int or float) and round if appropriate.
        :param inputvalue: A value within inputrange.
        :param inputrange: A 2-tuple that specifies the input range.
        :param outputrange: A 2-tuple that specifies the output range.
        :param crop: If the output value falls outside the outputrange, crop to legal value.
        :return: The output value within outputrange and of the same type as the outputrange members.
        """
        outputmin, outputmax = outputrange
        inputmin, inputmax = inputrange
        scale = float(outputmax - outputmin) / (inputmax - inputmin)
        ret = (inputvalue - inputmin) * scale + outputmin
        if crop:
            if outputmin > outputmax:
                outputmin, outputmax = (outputmax, outputmin)
            ret = max(ret, outputmin)
            ret = min(ret, outputmax)
        # if the whole outputrange has a certain type, convert the output to that type
        if type(outputmin) == type(outputmax):
            if type(outputmin) == int:  # in case of int, round before casting
                return int(round(ret))
            else:  # otherwise just cast ... should be float but you never know
                return type(outputmin)(ret)
        else:
            return ret


if __name__ == '__main__':
    watcher = HowToGivePowerConsumptionColorFeedbackViaRSB()
    watcher.run()
