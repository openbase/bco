# ============================================================
#
# Copyright (C) 2010 by Johannes Wienke <jwienke at techfak dot uni-bielefeld dot de>
# Copyright (C) 2011-2016 Jan Moringen <jmoringe@techfak.uni-bielefeld.de>
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
This module contains different transport implementations for RSB and their
common base classes and utility functions.

.. codeauthor:: jmoringe
.. codeauthor:: jwienke
"""

import abc
import copy
import threading

from rsb.util import getLoggerByClass
import logging


class Connector(object):
    """
    Superclass for transport-specific connector classes.

    .. codeauthor:: jwienke
    """

    def __init__(self, wireType=None, **kwargs):
        """
        Creates a new connector with a serialization type wireType.

        Args:
            wireType (types.TypeType):
                the type of serialized data used by this connector.
        """
        self.__logger = getLoggerByClass(self.__class__)

        self.__wireType = None
        self.__scope = None

        if wireType is None:
            raise ValueError("Wire type must be a type object, None given")

        self.__logger.debug("Using specified converter map for wire-type %s",
                            wireType)
        self.__wireType = wireType

        # fails if still some arguments are left over
        super(Connector, self).__init__(**kwargs)

    def getWireType(self):
        """
        Returns the serialization type used for this connector.

        Returns:
            python serialization type
        """
        return self.__wireType

    wireType = property(getWireType)

    def getScope(self):
        return self.__scope

    def setScope(self, newValue):
        """
        Sets the scope this connector will receive events from to
        ``newValue``. Called before #activate.

        Args:
            newValue (rsb.Scope):
                scope of the connector
        """
        self.__scope = newValue

    scope = property(getScope, setScope)

    def activate(self):
        raise NotImplementedError()

    def deactivate(self):
        raise NotImplementedError()

    def setQualityOfServiceSpec(self, qos):
        raise NotImplementedError()


class InConnector(Connector):
    """
    Superclass for in-direction (that is, dealing with incoming
    events) connector implementations.

    .. codeauthor:: jmoringe
    """

    def filterNotify(self, filter, action):
        raise NotImplementedError()

    def setObserverAction(self, action):
        """
        Sets the action used by the connector to notify about incoming
        events. The call to this method must be thread-safe.

        Args:
            action:
                action called if a new message is received from the connector.
                Must accept an :obj:`Event` as parameter.
        """
        pass


class OutConnector(Connector):
    """
    Superclass for out-direction (that is, dealing with outgoing
    events) connector implementations.

    .. codeauthor:: jmoringe
    """

    def handle(self, event):
        """
        Sends ``event`` and adapts its meta data instance with the
        actual send time.

        Args:
            event:
                event to send
        """
        raise NotImplementedError()


class ConverterSelectingConnector(object):
    """
    This class is intended to be used a superclass (or rather mixin
    class) for connector classes which have to store a map of
    converters and select converters for (de)serialization.

    .. codeauthor:: jmoringe
    """

    def __init__(self, converters, **kwargs):
        """
        Creates a new connector that uses the converters in
        ``converters`` to deserialize notification and/or serialize
        events.

        Args:
            converters (rsb.converter.ConverterSelectionStrategy):
                The converter selection strategy that should be used by the
                connector. If ``None``, the global map of converters for the
                wire-type of the connector is used.
        """
        self.__converterMap = converters

        assert(self.__converterMap.getWireType() == self.wireType)

    def getConverterForDataType(self, dataType):
        """
        Returns a converter that can convert the supplied data to the
        wire-type.

        Args:
            dataType:
                the type of the object for which a suitable converter should
                returned.

        Returns:
            converter

        Raises:
            KeyError:
                no converter is available for the supplied data.
        """
        return self.__converterMap.getConverterForDataType(dataType)

    def getConverterForWireSchema(self, wireSchema):
        """
        Returns a suitable converter for the ``wireSchema``.

        Args:
            wireSchema (str):
                the wire-schema to or from which the returned converter should
                convert

        Returns:
            converter

        Raises:
            KeyError:
                no converter is available for the specified wire-schema.

        """
        return self.__converterMap.getConverterForWireSchema(wireSchema)

    def getConverterMap(self):
        return self.__converterMap

    converterMap = property(getConverterMap)


class TransportFactory(object):
    """
    Interface for factories which are able to create :obj:`Connector` instances
    for a certain transport.
    """

    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def getName(self):
        """
        Returns the name representing this transport.

        Returns:
            str:
                name of the transport, non-empty
        """
        pass

    @abc.abstractmethod
    def isRemote(self):
        """
        Returns true is the transport performs remote communication.

        Returns:
            bool:
                does the transport perform remote communication?
        """
        pass

    @abc.abstractmethod
    def createInPushConnector(self, converters, options):
        """
        Creates a new instance of an :obj:`InConnector` for the represented
        transport.

        Args:
            converters (ConverterSelectionStrategy):
                the converters to use for this type options(dict of str):
                options for the new connector

        Returns:
            rsb.transport.InConnector:
                the new connector instance
        """
        pass

    @abc.abstractmethod
    def createOutConnector(self, converters, options):
        """
        Creates a new instance of an :obj:`OutConnector` for the represented
        transport.

        Args:
            converters (ConverterSelectionStrategy):
                the converters to use for this type options(dict of str):
                options for the new connector

        Returns:
            rsb.transport.OutConnector:
                the new connector instance
        """
        pass


__factoriesByName = {}
__factoryLock = threading.Lock()


def registerTransport(factory):
    """
    Registers a new transport.

    Args:
        factory (rsb.transport.TransportFactory):
            the factory for the transport

    Raises:
        ValueError:
            there is already a transport registered with this name or the given
            factory argument is invalid

    """

    if factory is None:
        raise ValueError("None cannot be a TransportFactory")
    with __factoryLock:
        if factory.getName() in __factoriesByName:
            raise ValueError(
                "There is already a transport with name {name}".format(
                    name=factory.getName()))
        __factoriesByName[factory.getName()] = factory


def getTransportFactory(name):
    """
    Returns a ``TransportFactory`` instance for the transport with the given
    name.

    Args:
        name (str):
            name of the transport

    Returns:
        rsb.transport.TransportFactory:
            the ``TransportFactory`` instance

    Raises:
        KeyError:
            there is not transport with the given name
    """
    with __factoryLock:
        return __factoriesByName[name]
