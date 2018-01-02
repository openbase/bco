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
This package contains a transport implementation based on the spread toolkit
which uses a multicased-based daemon network.

.. codeauthor:: jmoringe
.. codeauthor:: jwienke
"""

import threading
from threading import RLock
import uuid
import hashlib
import math
import logging
import time

import spread

import rsb
import rsb.util
import rsb.filter
import rsb.transport
import rsb.converter

from rsb.protocol.FragmentedNotification_pb2 import FragmentedNotification
from google.protobuf.message import DecodeError

import rsb.transport.conversion as conversion


def makeKey(notification):
    key = notification.event_id.sender_id + '%08x' \
        % notification.event_id.sequence_number
    return key


class Assembly(object):
    """
    A class that maintains a collection of fragments of one fragmented
    notification and assembles them if all fragments are received.

    .. codeauthor:: jwienke
    """

    def __init__(self, fragment):
        self.__requiredParts = fragment.num_data_parts
        assert(self.__requiredParts > 1)
        self.__id = makeKey(fragment.notification)
        self.__parts = {fragment.data_part: fragment}

    def add(self, fragment):
        key = makeKey(fragment.notification)
        assert(key == self.__id)
        if fragment.data_part in self.__parts:
            raise ValueError(
                "Received part %u for notification with id %s again."
                % (fragment.data_part, key))

        self.__parts[fragment.data_part] = fragment

        if len(self.__parts) == self.__requiredParts:
            return (self.__parts[0].notification, self.__join(),
                    self.__parts[0].notification.wire_schema)
        else:
            return None

    def __join(self):
        keys = self.__parts.keys()
        keys.sort()
        finalData = bytearray()
        for key in keys:
            finalData += bytearray(self.__parts[key].notification.data)
        return finalData


class AssemblyPool(object):
    """
    Maintains the parallel joining of notification fragments that are
    received in an interleaved fashion.

    .. codeauthor:: jwienke
    """

    def __init__(self):
        self.__assemblies = {}

    def add(self, fragment):
        notification = fragment.notification
        if fragment.num_data_parts == 1:
            return (notification, bytearray(notification.data),
                    notification.wire_schema)
        key = makeKey(notification)
        if key not in self.__assemblies:
            self.__assemblies[key] = Assembly(fragment)
            return None
        else:
            result = self.__assemblies[key].add(fragment)
            if result is not None:
                del self.__assemblies[key]
                return result


class SpreadConnection(object):
    """
    A wrapper around a spread mailbox for some convenience.

    .. codeauthor:: jwienke
    """

    def __init__(self, daemonName, spreadModule=spread):
        self.__daemonName = daemonName
        self.__spreadModule = spreadModule
        self.__mailbox = None

    def activate(self):
        if self.__mailbox is not None:
            raise ValueError("Already activated")
        self.__mailbox = self.__spreadModule.connect(self.__daemonName)

    def deactivate(self):
        if self.__mailbox is None:
            raise ValueError("Not activated")
        self.__mailbox.disconnect()
        self.__mailbox = None

    def __getattr__(self, name):
        """
        Dispatches everything that is not implemented in here to the spread
        mailbox object.
        """
        if self.__mailbox is None:
            raise ValueError("Not activated")
        return getattr(self.__mailbox, name)

    def getHost(self):
        name = self.__daemonName.split('@')
        return name[1] if '@' in name else 'localhost'

    def getPort(self):
        return int(self.__daemonName.split('@')[0])


class RefCountingSpreadConnection(SpreadConnection):

    def __init__(self, daemonName, spreadModule=spread):
        SpreadConnection.__init__(self, daemonName, spreadModule=spreadModule)
        self.__lock = threading.RLock()
        self.__counter = 0

    def activate(self):
        with self.__lock:
            if self.__counter == 0:
                SpreadConnection.activate(self)
            self.__counter += 1

    def deactivate(self):
        with self.__lock:
            if self.__counter <= 0:
                raise ValueError("deactivate called more times than activate")
            self.__counter -= 1
            if self.__counter == 0:
                SpreadConnection.deactivate(self)


class SpreadReceiverTask(object):
    """
    Thread used to receive messages from a spread connection.

    .. codeauthor:: jwienke
    """

    def __init__(self, mailbox, observerAction, converterMap):
        """
        Constructor.

        Args:
            mailbox:
                spread mailbox to receive from
            observerAction:
                callable to execute if a new event is received
            converterMap:
                converters for data
        """

        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        self.__interrupted = False
        self.__interruptionLock = threading.RLock()

        self.__mailbox = mailbox
        self.__observerAction = observerAction
        self.__observerActionLock = RLock()

        self.__converterMap = converterMap
        assert(converterMap.getWireType() == bytearray)

        self.__taskId = uuid.uuid1()
        # narf, spread groups are 32 chars long but 0-terminated... truncate id
        self.__wakeupGroup = str(self.__taskId).replace("-", "")[:-1]

        self.__assemblyPool = AssemblyPool()

    def __call__(self):

        # join my id to receive interrupt messages.
        # receive cannot have a timeout, hence we need a way to stop receiving
        # messages on interruption even if no one else sends messages.
        # Otherwise deactivate blocks until another message is received.
        self.__mailbox.join(self.__wakeupGroup)
        self.__logger.debug("joined wakup group %s", self.__wakeupGroup)

        while True:

            # check interruption
            self.__interruptionLock.acquire()
            interrupted = self.__interrupted
            self.__interruptionLock.release()

            if interrupted:
                break

            self.__logger.debug("waiting for new messages")
            message = self.__mailbox.receive()
            self.__logger.debug("received message %s", message)
            try:

                # Process regular message
                if isinstance(message, spread.RegularMsgType):
                    # ignore the deactivate wakeup message
                    if self.__wakeupGroup in message.groups:
                        continue

                    try:
                        fragment = FragmentedNotification()
                        fragment.ParseFromString(message.message)
                    except DecodeError:
                        self.__logger.exception("Error decoding notification")
                        continue

                    if self.__logger.isEnabledFor(logging.DEBUG):
                        self.__logger.debug(
                            "Received notification fragment "
                            "from bus (%s/%s), data length: %s",
                            fragment.data_part,
                            fragment.num_data_parts,
                            len(fragment.notification.data))

                    assembled = self.__assemblyPool.add(fragment)
                    if assembled:
                        notification, joinedData, wireSchema = assembled
                        # Create event from (potentially assembled)
                        # notification(s)
                        converter = \
                            self.__converterMap.getConverterForWireSchema(
                                wireSchema)
                        try:
                            event = conversion.notificationToEvent(
                                notification, joinedData, wireSchema,
                                converter)
                        except Exception:
                            self.__logger.exception(
                                "Unable to decode event. "
                                "Ignoring and continuing.")
                            continue

                        self.__logger.debug(
                            "Sending event to dispatch task: %s", event)

                        with self.__observerActionLock:
                            if self.__observerAction:
                                self.__observerAction(event)

                # Process membership message
                elif isinstance(message, spread.MembershipMsgType):
                    self.__logger.info(
                        "Received membership message for group `%s'",
                        message.group)

            except Exception, e:
                self.__logger.exception("Error processing new event")
                raise e

        # leave task id group to clean up
        self.__mailbox.leave(self.__wakeupGroup)

    def interrupt(self):
        self.__interruptionLock.acquire()
        self.__interrupted = True
        self.__interruptionLock.release()

        # send the interruption message to wake up receive as mentioned above
        self.__mailbox.multicast(spread.RELIABLE_MESS, self.__wakeupGroup, "")

    def setObserverAction(self, action):
        with self.__observerActionLock:
            self.__observerAction = action


class Connector(rsb.transport.Connector,
                rsb.transport.ConverterSelectingConnector):
    """
    Superclass for Spread-based connector classes. This class manages
    the direction-independent aspects like the Spread connection and
    (de)activation.

    .. codeauthor:: jwienke
    """

    MAX_MSG_LENGTH = 100000

    def __init__(self, connection, **kwargs):
        super(Connector, self).__init__(wireType=bytearray, **kwargs)

        self.__logger = rsb.util.getLoggerByClass(self.__class__)
        self.__connection = connection

        self.__active = False

        self.setQualityOfServiceSpec(rsb.QualityOfServiceSpec())

    def __del__(self):
        if self.__active:
            self.deactivate()

    def getConnection(self):
        return self.__connection

    connection = property(getConnection)

    def isActive(self):
        return self.__active

    active = property(isActive)

    def _getMsgType(self):
        return self.__msgType

    _msgType = property(_getMsgType)

    def activate(self):
        if self.__active:
            raise RuntimeError('Trying to activate active Connector')

        self.__logger.info("Activating spread connector with connection %s",
                           self.__connection)

        try:
            self.__connection.activate()
        except Exception, e:
            raise RuntimeError('Could not connect SpreadConnection "%s": %s' %
                               (self.__connection, e))

        self.__active = True

    def deactivate(self):
        if not self.__active:
            raise RuntimeError('Trying to deactivate inactive Connector')

        self.__logger.info("Deactivating spread connector")

        self.__active = False

        self.__connection.deactivate()

        self.__logger.debug("SpreadConnector deactivated")

    @staticmethod
    def _groupName(scope):
        hashSum = hashlib.md5()
        hashSum.update(scope.toString())
        return hashSum.hexdigest()[:-1]

    def setQualityOfServiceSpec(self, qos):
        self.__logger.debug("Adapting service type for QoS %s", qos)
        if qos.getReliability() == rsb.QualityOfServiceSpec.Reliability.UNRELIABLE \
           and qos.getOrdering() == rsb.QualityOfServiceSpec.Ordering.UNORDERED:
            self.__msgType = spread.UNRELIABLE_MESS
            self.__logger.debug("Chosen service type is UNRELIABLE_MESS,  value = %s",
                                self.__msgType)
        elif qos.getReliability() == rsb.QualityOfServiceSpec.Reliability.UNRELIABLE \
             and qos.getOrdering() == rsb.QualityOfServiceSpec.Ordering.ORDERED:
            self.__msgType = spread.FIFO_MESS
            self.__logger.debug("Chosen service type is FIFO_MESS,  value = %s",
                                self.__msgType)
        elif qos.getReliability() == rsb.QualityOfServiceSpec.Reliability.RELIABLE \
             and qos.getOrdering() == rsb.QualityOfServiceSpec.Ordering.UNORDERED:
            self.__msgType = spread.RELIABLE_MESS
            self.__logger.debug("Chosen service type is RELIABLE_MESS,  value = %s",
                                self.__msgType)
        elif qos.getReliability() == rsb.QualityOfServiceSpec.Reliability.RELIABLE \
             and qos.getOrdering() == rsb.QualityOfServiceSpec.Ordering.ORDERED:
            self.__msgType = spread.FIFO_MESS
            self.__logger.debug("Chosen service type is FIFO_MESS,  value = %s",
                                self.__msgType)
        else:
            assert(False)

    def getTransportURL(self):
        return 'spread://' \
            + self.__connection.getHost()  \
            + ':' + str(self.__connection.getPort())

class InConnector(Connector,
                  rsb.transport.InConnector):
    def __init__(self, **kwargs):
        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        self.__receiveThread = None
        self.__receiveTask = None
        self.__observerAction = None

        self.__scope = None

        super(InConnector, self).__init__(**kwargs)

    def setScope(self, scope):
        self.__logger.debug("Got new scope %s", scope)
        self.__scope = scope

    def activate(self):
        super(InConnector, self).activate()

        assert self.__scope is not None
        self.connection.join(self._groupName(self.__scope))

        self.__receiveTask = SpreadReceiverTask(self.connection,
                                                self.__observerAction,
                                                self.converterMap)
        self.__receiveThread = threading.Thread(target=self.__receiveTask)
        self.__receiveThread.setDaemon(True)
        self.__receiveThread.start()

    def deactivate(self):
        self.__receiveTask.interrupt()
        self.__receiveThread.join(timeout=1)
        self.__receiveThread = None
        self.__receiveTask = None

        super(InConnector, self).deactivate()

    def filterNotify(self, theFilter, action):
        self.__logger.debug("Ignoring filter %s with action %s",
                            theFilter, action)

    def setObserverAction(self, observerAction):
        self.__observerAction = observerAction
        if self.__receiveTask is not None:
            self.__logger.debug("Passing observer %s to receive task",
                                observerAction)
            self.__receiveTask.setObserverAction(observerAction)
        else:
            self.__logger.debug("Storing observer %s until activation",
                                observerAction)


class OutConnector(Connector,
                   rsb.transport.OutConnector):
    def __init__(self, **kwargs):
        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        super(OutConnector, self).__init__(**kwargs)

    def handle(self, event):
        self.__logger.debug("Sending event: %s", event)

        if not self.active:
            self.__logger.warning("Connector not activated")
            return

        # Create one or more notification fragments for the event
        event.getMetaData().setSendTime()
        converter = self.getConverterForDataType(event.type)
        fragments = conversion.eventToNotifications(
            event, converter, self.MAX_MSG_LENGTH)

        # Send fragments
        self.__logger.debug("Sending %u fragments", len(fragments))
        for (i, fragment) in enumerate(fragments):
            serialized = fragment.SerializeToString()
            self.__logger.debug("Sending fragment %u of length %u",
                                i + 1, len(serialized))

            # TODO respect QoS
            scopes = event.scope.superScopes(True)
            groupNames = [self._groupName(scope) for scope in scopes]
            self.__logger.debug("Sending to scopes %s which are groupNames %s",
                                scopes, groupNames)

            sent = self.connection.multigroup_multicast(self._msgType,
                                                        tuple(groupNames),
                                                        serialized)
            if (sent > 0):
                self.__logger.debug("Message sent successfully (bytes = %i)",
                                    sent)
            else:
                # TODO(jmoringe): propagate error
                self.__logger.warning(
                    "Error sending message, status code = %s", sent)


class TransportFactory(rsb.transport.TransportFactory):
    """
    :obj:`TransportFactory` implementation for the spread transport.

    .. codeauthor:: jwienke
    """

    def __init__(self):
        self.__lock = threading.RLock()
        self.__connectionByDaemon = {}

    def getName(self):
        return "spread"

    def isRemote(self):
        return True

    @staticmethod
    def __createDaemonName(options):

        host = options.get('host', None)
        port = options.get('port', '4803')
        if host:
            return '%s@%s' % (port, host)
        else:
            return port

    def __getSharedConnection(self, daemonName):
        with self.__lock:
            if daemonName not in self.__connectionByDaemon:
                self.__connectionByDaemon[daemonName] = \
                    RefCountingSpreadConnection(daemonName)
            return self.__connectionByDaemon[daemonName]

    def createInPushConnector(self, converters, options):
        return InConnector(connection=SpreadConnection(
            self.__createDaemonName(options)), converters=converters)

    def createOutConnector(self, converters, options):
        return OutConnector(connection=self.__getSharedConnection(
            self.__createDaemonName(options)), converters=converters)


def initialize():
    try:
        rsb.transport.registerTransport(TransportFactory())
    except ValueError:
        pass
