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
A module containing various converter implementations as well as logic for
registering and selecting them.

.. codeauthor:: jmoringe
.. codeauthor:: jwienke
.. codeauthor:: plueckin
"""

from numbers import Integral, Real
from threading import RLock
import struct
import abc

from rsb.protocol.collections.EventsByScopeMap_pb2 import EventsByScopeMap
from rsb.transport.conversion import notificationToEvent, eventToNotification
from rsb import Scope


class Converter(object):
    """
    Base class for converters to a certain target type.

    .. codeauthor:: jwienke
    """

    def __init__(self, wireType, dataType, wireSchema):
        """
        Constructor.

        Args:
            wireType (types.TypeType):
                Python type to/from which the converter serializes/deserializes
            dataType (types.TypeType):
                Python type of data accepted by the converter for serialization
                (also Python type of deserialized data)
            wireSchema (str):
                Wire-schema understood by the converter when deserializing
                (also wire-schema of data serialized with the converter)
        """
        self.__wireType = wireType
        self.__dataType = dataType
        self.__wireSchema = wireSchema

    def getWireType(self):
        """
        Returns the type of the wire-type to/from this converter
        serializes/deserializes.

        Returns:
            types.TypeType:
                A type object.
        """
        return self.__wireType

    wireType = property(getWireType)

    def getDataType(self):
        """
        Returns the data type this converter is applicable for.

        Returns:
            types.TypeType:
                A type object.
        """
        return self.__dataType

    dataType = property(getDataType)

    def getWireSchema(self):
        """
        Returns the name of the wire schema this converter can (de)serialize
        from/to.

        Returns:
            str:
                A string designating the wire schema from/to this converter can
                (de)serialize
        """
        return self.__wireSchema

    wireSchema = property(getWireSchema)

    def serialize(self, inp):
        raise NotImplementedError()

    def deserialize(self, inp, wireSchema):
        raise NotImplementedError()


class UnknownConverterError(KeyError):
    """
    .. codeauthor:: jwienke
    """

    def __init__(self, sourceType, wireSchema):
        KeyError.__init__(
            self,
            "No converter from type %s to type %s available" % (sourceType,
                                                                wireSchema))


class ConverterSelectionStrategy(object):
    """
    This class defines the interface for converter selection strategy
    classes.

    .. codeauthor:: jmoringe
    """
    def hasConverterForWireSchema(self, wireSchema):
        return bool(self._getConverterForWireSchema(wireSchema))

    def getConverterForWireSchema(self, wireSchema):
        converter = self._getConverterForWireSchema(wireSchema)
        if converter:
            return converter
        raise KeyError(wireSchema)

    def hasConverterForDataType(self, dataType):
        return bool(self._getConverterForDataType(dataType))

    def getConverterForDataType(self, dataType):
        converter = self._getConverterForDataType(dataType)
        if converter:
            return converter
        raise KeyError(dataType)

    @abc.abstractmethod
    def _getConverterForWireSchema(self, wireSchma):
        pass

    @abc.abstractmethod
    def _getConverterForDataType(self, dataType):
        pass


class ConverterMap(ConverterSelectionStrategy):
    """
    A class managing converters for for a certain target type.

    .. codeauthor:: jwienke
    """

    def __init__(self, wireType):
        self._wireType = wireType
        self._converters = {}

    def getWireType(self):
        return self._wireType

    def addConverter(self, converter, replaceExisting=False):
        key = (converter.getWireSchema(), converter.getDataType())
        if key in self._converters and not replaceExisting:
            raise RuntimeError("There already is a converter "
                               "with wire-schema `%s' and data-type `%s'"
                               % key)
        self._converters[key] = converter

    def _getConverterForWireSchema(self, wireSchema):
        for ((converterWireSchema, _), converter) in self._converters.items():
            if converterWireSchema == wireSchema:
                return converter

    def _getConverterForDataType(self, dataType):
        # If multiple converters are applicable, use most specific.
        candidates = []
        for ((_, converterDataType), converter) in self._converters.items():
            if issubclass(dataType, converterDataType):
                candidates.append(converter)
        if len(candidates) == 1:
            return candidates[0]
        elif len(candidates) > 1:
            def compareViaSubclass(x, y):
                if issubclass(x, y):
                    return -1
                else:
                    return 1
            return sorted(candidates, compareViaSubclass,
                          key=lambda x: x.getDataType())[0]

    def getConverters(self):
        return self._converters

    def __str__(self):
        s = "ConverterMap(wireType = %s):\n" % self._wireType
        for converter in self._converters.values():
            s = s + ("\t%s <-> %s\n" % (converter.getWireSchema(),
                                        converter.getDataType()))
        return s[:-1]


class PredicateConverterList(ConverterMap):
    """
    Objects of this class are used to perform converter selection via
    a chain-of-responsibility strategy.

    A list of predicates and associated converters is
    maintained. Converter selection queries are processed by
    traversing the list and selected the first converter the
    associated predicate of which matches the query wire-schema or
    data-type.

    .. codeauthor:: jmoringe
    """
    def __init__(self, wireType):
        super(PredicateConverterList, self).__init__(wireType)
        self._list = []

    def addConverter(self, converter,
                     wireSchemaPredicate=None,
                     dataTypePredicate=None,
                     replaceExisting=True):
        if wireSchemaPredicate is None:
            # if converter.getWireSchema() == 'void':
            #    wireSchemaPredicate = lambda wireSchema: True
            # else:
            wireSchemaPredicate = lambda wireSchema: \
                wireSchema == converter.getWireSchema()
        if dataTypePredicate is None:
            dataTypePredicate = lambda dataType: \
                dataType == converter.getDataType()
        key = (wireSchemaPredicate, dataTypePredicate)
        self._converters[key] = converter
        self._list.append((key, converter))

    def _getConverterForWireSchema(self, wireSchema):
        for ((predicate, _), converter) in self._list:
            if predicate(wireSchema):
                return converter

    def _getConverterForDataType(self, dataType):
        for ((_, predicate), converter) in self._list:
            if predicate(dataType):
                return converter


class UnambiguousConverterMap(ConverterMap):
    def __init__(self, wireType):
        super(UnambiguousConverterMap, self).__init__(wireType)

    def addConverter(self, converter, replaceExisting=False):
        for (wireSchema, dataType) in self.getConverters().keys():
            if wireSchema == converter.getWireSchema():
                if dataType == converter.getDataType():
                    super(UnambiguousConverterMap, self).addConverter(
                        converter, replaceExisting)
                else:
                    raise RuntimeError(
                        "Trying to register ambiguous converter "
                        "with data type `%s' for wire-schema `%s' "
                        "(present converter is for data type `%s')."
                        % (converter.getDataType(),
                           wireSchema,
                           dataType))
        super(UnambiguousConverterMap, self).addConverter(
            converter, replaceExisting)

__globalConverterMapsLock = RLock()
__globalConverterMaps = {}


def registerGlobalConverter(converter, replaceExisting=False):
    """
    Register ``converter`` as a globally available converter.

    Args:
        converter:
            converter to register
        replaceExisting:
            controls whether an existing converter for the same data-type
            and/or wire-type should be replaced by the new converter. If this
            is ``False`` and such a converter exists, an error is raised.
    """
    mapForWireType = getGlobalConverterMap(converter.getWireType())
    mapForWireType.addConverter(converter, replaceExisting)


def getGlobalConverterMap(wireType):
    """
    Get a map with all globally known converters for the ``wireType``.

    Args:
        wireType (types.TypeType):
            Python type for designating the wire-type.

    Returns:
        converter map constantly updated
    """

    with __globalConverterMapsLock:
        if wireType not in __globalConverterMaps:
            __globalConverterMaps[wireType] = ConverterMap(wireType)
        return __globalConverterMaps[wireType]

# --- converters with bytearray as serialization type ---


class IdentityConverter(Converter):
    """
    This converter does nothing. Use it in combination with the
    "AlwaysApplicable"-wireSchema.

    .. codeauthor:: plueckin
    """
    def __init__(self):
        super(IdentityConverter, self).__init__(bytearray, type(None), 'void')

    def serialize(self, inp):
        return bytearray(), self.wireSchema

    def deserialize(self, inp, wireSchema):
        pass

    def AlwaysApplicable(self):
        return bytearray


class NoneConverter(Converter):
    """
    This converter produces a serialized value that represents
    instances of ``NoneType``.

    Such a converter is required for serializing "results" of RPC
    calls that do not return a value.

    .. codeauthor:: jmoringe
    """
    def __init__(self):
        super(NoneConverter, self).__init__(bytearray, type(None), 'void')

    def serialize(self, inp):
        return bytearray(), self.wireSchema

    def deserialize(self, inp, wireSchema):
        assert wireSchema == self.wireSchema


def makeStructBasedConverter(name, dataType, wireSchema, fmt, size):
    class NewConverter(Converter):
        def __init__(self):
            super(self.__class__, self).__init__(
                bytearray, dataType, wireSchema)

        def serialize(self, inp):
            return bytearray(struct.pack(fmt, inp)), self.wireSchema

        def deserialize(self, inp, wireSchema):
            assert wireSchema == self.wireSchema
            return struct.unpack(fmt, str(inp))[0]

    NewConverter.__name__ = name
    # TODO(jmoringe): seems to be impossible in CPython
    # NewConverter.__doc__ = """
    # A converter that serializes %(dataType)s to bytearrays with
    # %(wireSchema)s wire-schema.
    #
    # .. codeauthor:: jmoringe
    # """ % {
    #     "dataType":   dataType,
    #     "wireSchema": wireSchema
    # }

    globals()[name] = NewConverter
    return NewConverter

makeStructBasedConverter('DoubleConverter', Real, 'double', '<d', 8)
makeStructBasedConverter('FloatConverter', Real, 'float', '<f', 4)
makeStructBasedConverter('Uint32Converter', Integral, 'uint32', '<I', 4)
makeStructBasedConverter('Int32Converter', Integral, 'int32', '<i', 4)
makeStructBasedConverter('Uint64Converter', Integral, 'uint64', '<Q', 8)
makeStructBasedConverter('Int64Converter', Integral, 'int64', '<q', 8)
makeStructBasedConverter('BoolConverter', bool, 'bool', '?', 1)


# Registered at end of file
class BytesConverter(Converter):
    """
    Handles byte arrays.

    .. codeauthor:: jmoringe
    """

    def __init__(self, wireSchema="bytes", dataType=bytearray):
        super(BytesConverter, self).__init__(bytearray, dataType, wireSchema)

    def serialize(self, inp):
        return inp, self.wireSchema

    def deserialize(self, inp, wireSchema):
        assert(wireSchema == self.wireSchema)
        return inp


class StringConverter(Converter):
    """
    A converter that serializes strings to bytearrays with a specified
    encoding

    .. codeauthor:: jwienke
    """

    def __init__(self,
                 wireSchema="utf-8-string",
                 dataType=unicode,
                 encoding="utf_8"):
        super(StringConverter, self).__init__(bytearray, dataType, wireSchema)
        self.__encoding = encoding

    def serialize(self, inp):
        return bytearray(inp.encode(self.__encoding)), self.wireSchema

    def deserialize(self, inp, wireSchema):
        dataType = self.getDataType()
        if dataType == unicode:
            return dataType(str(inp), self.__encoding)
        elif dataType == str:
            return str(inp)
        else:
            raise ValueError("Inacceptable dataType %s" % type)


class ByteArrayConverter(Converter):
    """
    A converter which just passes through the original byte array of a message.

    .. codeauthor:: jwienke
    """
    def __init__(self):
        super(ByteArrayConverter, self).__init__(bytearray, bytearray, '.*')

    def serialize(self, data):
        return bytearray(data), self.wireSchema

    def deserialize(self, data, wireSchema):
        return bytearray(data)


class SchemaAndByteArrayConverter(Converter):
    """
    A converter which passes through the wireSchema as well as the original
    byte array of a message.

    .. codeauthor:: nkoester
    """
    def __init__(self):
        super(SchemaAndByteArrayConverter, self).__init__(
            bytearray, tuple, '.*')

    def serialize(self, data):
        return data[1], data[0]

    def deserialize(self, data, wireSchema):
        return wireSchema, data


class ProtocolBufferConverter(Converter):
    """
    This converter serializes and deserializes objects of protocol
    buffer data-holder classes.

    These data-holder classes are generated by the protocol buffer
    compiler protoc.

    .. codeauthor:: jmoringe
    """
    def __init__(self, messageClass):
        super(ProtocolBufferConverter, self).__init__(
            bytearray, messageClass, '.%s' % messageClass.DESCRIPTOR.full_name)

        self.__messageClass = messageClass

    def getMessageClass(self):
        return self.__messageClass

    messageClass = property(getMessageClass)

    def getMessageClassName(self):
        return self.messageClass.DESCRIPTOR.full_name

    def serialize(self, inp):
        return bytearray(inp.SerializeToString()), self.wireSchema

    def deserialize(self, inp, wireSchema):
        assert wireSchema == self.wireSchema

        output = self.messageClass()
        # we need to convert back to string because bytearrays do not work with
        # protobuf
        output.ParseFromString(str(inp))
        return output

    def __str__(self):
        return '<%s for %s at 0x%x>' \
            % (type(self).__name__, self.getMessageClassName(), id(self))

    def __repr__(self):
        return str(self)


class ScopeConverter(Converter):
    """
    (De)serializes :obj:`Scope` objects.

    .. codeauthor:: jmoringe
    """

    def __init__(self):
        super(ScopeConverter, self).__init__(bytearray, Scope, 'scope')

    def serialize(self, inp):
        return bytearray(inp.toString().encode(encoding='ascii')), \
            self.wireSchema

    def deserialize(self, inp, wireSchema):
        assert wireSchema == self.wireSchema

        return Scope(str(inp))


class EventsByScopeMapConverter(Converter):
    """
    A converter for aggregated events ordered by their scope and time for each
    scope. As a client data type dictionaries are used. Think about this when
    you register the converter and also have other dictionaries to transmit.

    .. codeauthor:: jwienke
    """

    def __init__(self, converterRepository=getGlobalConverterMap(bytearray)):
        self.__converterRepository = converterRepository
        self.__converter = ProtocolBufferConverter(EventsByScopeMap)
        super(EventsByScopeMapConverter, self).__init__(
            bytearray, dict, self.__converter.wireSchema)

    def serialize(self, data):

        eventMap = EventsByScopeMap()

        for scope, events in data.iteritems():

            scopeSet = eventMap.sets.add()
            scopeSet.scope = scope.toString()

            for event in events:

                wire, wireSchema = \
                    self.__converterRepository.getConverterForDataType(
                        type(event.data)).serialize(event.data)

                notification = scopeSet.notifications.add()
                eventToNotification(notification, event,
                                    wireSchema, wire, True)

        return self.__converter.serialize(eventMap)

    def deserialize(self, wire, wireSchema):
        preliminaryMap = self.__converter.deserialize(wire, wireSchema)

        output = {}

        for scopeSet in preliminaryMap.sets:
            scope = Scope(scopeSet.scope)
            output[scope] = []
            for notification in scopeSet.notifications:

                converter = \
                    self.__converterRepository.getConverterForWireSchema(
                        notification.wire_schema)
                event = notificationToEvent(
                    notification, notification.data,
                    notification.wire_schema, converter)

                output[scope].append(event)

        return output

# FIXME We do not register all available converters here to avoid
# ambiguities.
registerGlobalConverter(NoneConverter())
registerGlobalConverter(DoubleConverter())
# registerGlobalConverter(FloatConverter())
# registerGlobalConverter(Uint32Converter())
# registerGlobalConverter(Int32Converter())
# registerGlobalConverter(Uint64Converter())
registerGlobalConverter(Int64Converter())
registerGlobalConverter(BoolConverter())
registerGlobalConverter(BytesConverter())
registerGlobalConverter(StringConverter(wireSchema="utf-8-string",
                                        dataType=str, encoding="utf_8"))
registerGlobalConverter(ByteArrayConverter())
registerGlobalConverter(ScopeConverter())
