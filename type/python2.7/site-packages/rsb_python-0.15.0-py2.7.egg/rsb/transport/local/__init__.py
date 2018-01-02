# ============================================================
#
# Copyright (C) 2012 by Johannes Wienke <jwienke at techfak dot uni-bielefeld dot de>
#
# This file may be licensed under the terms of the
# GNU Lesser General Public License Version 3 (the ``LGPL''),
# or (at your option) any later version.
#
# Software distributed under the License is distributed
# on an ``AS IS'' basis, WITHOUT WARRANTY OF ANY KIND, either
# express or implied. See the LGPL for the specific language
# governing rights and limitations.
#
# You should have received a copy of the LGPL along with this
# program. If not, go to http://www.gnu.org/licenses/lgpl.html
# or write to the Free Software Foundation, Inc.,
# 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
#
# The development of this software was supported by:
#   CoR-Lab, Research Institute for Cognition and Robotics
#     Bielefeld University
#
# ============================================================

"""
This package contains a highly efficient in-process transport implementation
which allows participants inside one python process to communicate without
serialization overhead.

.. codeauthor:: jwienke
"""

import os
import platform
from threading import RLock

from rsb import transport


class Bus(object):
    """
    Singleton-like representation of the local bus.

    .. codeauthor:: jwienke
    """

    def __init__(self):
        self.__mutex = RLock()
        self.__sinksByScope = {}

    def addSink(self, sink):
        """
        Adds a sink for events pushed to the Bus.

        Args:
            sink:
                the sink to add
        """
        with self.__mutex:
            # ensure that there is a list of sinks for the given scope
            if sink.getScope() not in self.__sinksByScope:
                self.__sinksByScope[sink.getScope()] = []
            self.__sinksByScope[sink.getScope()].append(sink)

    def removeSink(self, sink):
        """
        Removes a sink to not be notified anymore.

        Args:
            sink:
                sink to remove
        """
        with self.__mutex:
            # return immediately if there is no such scope known for sinks
            if sink.getScope() not in self.__sinksByScope:
                return
            self.__sinksByScope[sink.getScope()].remove(sink)

    def handle(self, event):
        """
        Dispatches the provided event to all sinks of the appropriate scope.

        Args:
            event (rsb.Event):
                event to dispatch
        """

        with self.__mutex:

            for scope, sinkList in self.__sinksByScope.items():
                if scope == event.scope or scope.isSuperScopeOf(event.scope):
                    for sink in sinkList:
                        sink.handle(event)

    def getTransportURL(self):
        hostname = platform.node().split('.')[0]
        pid = os.getpid()
        return 'inprocess://' + hostname + ':' + str(pid)

globalBus = Bus()


class OutConnector(transport.OutConnector):
    """
    In-process OutConnector.

    .. codeauthor:: jwienke
    """

    def __init__(self, bus=globalBus, converters=None, options=None, **kwargs):
        # pylint: disable=unused-argument
        transport.OutConnector.__init__(self, wireType=object, **kwargs)
        self.__bus = bus

    def handle(self, event):
        event.metaData.setSendTime()
        self.__bus.handle(event)

    def activate(self):
        pass

    def deactivate(self):
        pass

    def setQualityOfServiceSpec(self, qos):
        pass

    def getTransportURL(self):
        return self.__bus.getTransportURL()


class InConnector(transport.InConnector):
    """
    InConnector for the local transport.

    .. codeauthor:: jwienke
    """

    def __init__(self, bus=globalBus, converters=None, options=None, **kwargs):
        # pylint: disable=unused-argument
        transport.InConnector.__init__(self, wireType=object, **kwargs)
        self.__bus = bus
        self.__scope = None
        self.__observerAction = None

    def filterNotify(self, filter, action):
        pass

    def setObserverAction(self, action):
        self.__observerAction = action

    def setScope(self, scope):
        self.__scope = scope

    def getScope(self):
        return self.__scope

    def activate(self):
        assert self.__scope is not None
        self.__bus.addSink(self)

    def deactivate(self):
        self.__bus.removeSink(self)

    def setQualityOfServiceSpec(self, qos):
        pass

    def handle(self, event):
        # get reference which will survive parallel changes to the action
        event.metaData.setReceiveTime()
        action = self.__observerAction
        if action is not None:
            action(event)

    def getTransportURL(self):
        return self.__bus.getTransportURL()


class TransportFactory(transport.TransportFactory):
    """
    :obj:`TransportFactory` implementation for the local transport.

    .. codeauthor:: jwienke
    """

    def getName(self):
        return "inprocess"

    def isRemote(self):
        return False

    def createInPushConnector(self, converters, options):
        return InConnector(converters=converters, options=options)

    def createOutConnector(self, converters, options):
        return OutConnector(converters=converters, options=options)


def initialize():
    try:
        transport.registerTransport(TransportFactory())
    except ValueError:
        pass
