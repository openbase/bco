# ============================================================
#
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
This package contains a transport implementation that uses multiple
point-to-point socket connections to simulate a bus.

.. codeauthor:: jmoringe
"""

import copy
import socket
import threading

import rsb.util
import rsb.transport
import rsb.transport.conversion as conversion

from rsb.protocol.EventId_pb2 import EventId
from rsb.protocol.EventMetaData_pb2 import UserInfo, UserTime
from rsb.protocol.Notification_pb2 import Notification


class BusConnection(rsb.eventprocessing.BroadcastProcessor):
    """
    Instances of this class implement connections to a socket-based
    bus.

    The basic operations provided by this class are receiving an event
    by calling :obj:`receiveNotification` and submitting an event to the bus by
    calling :obj:`sendNotification`.

    In a process which act as a client for a particular bus, a single
    instance of this class is connected to the bus server and provides
    access to the bus for the process.

    A process which acts as the server for a particular bus, manages
    (via the :obj:`BusServer` class) one :obj:`BusConnection` object for each
    client (remote process) connected to the bus.

    .. codeauthor:: jmoringe

    Args:

    Returns:

    """

    def __init__(self,
                 host=None, port=None, socket_=None,
                 isServer=False, tcpnodelay=True):
        """
        Args:
            host (str or None):
                Hostname or address of the bus server.
            port (int or None):
                Port of the bus server.
            socket_:
                A socket object through which the new connection should access
                the bus.
            isServer (bool):
                if True, the created object will perform the server part of the
                handshake protocol.
            tcpnodelay (bool):
                If True, the socket will be set to TCP_NODELAY.

        See Also:
            :obj:`getBusClientFor`, :obj:`getBusServerFor`.
        """
        super(BusConnection, self).__init__()

        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        self.__thread = None
        self.__socket = None
        self.__file = None

        self.__errorHook = None

        self.__active = False
        self.__activeShutdown = False

        self.__lock = threading.RLock()

        # Create a socket connection or store the provided connection.
        if host is not None and port is not None:
            if socket_ is None:
                self.__socket = socket.create_connection((host, port))
            else:
                raise ValueError('Specify either host and port or socket')
        elif socket_ is not None:
            self.__socket = socket_
        else:
            raise ValueError('Specify either host and port or socket_')
        if tcpnodelay:
            self.__socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        else:
            self.__socket.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 0)
        self.__file = self.__socket.makefile()

        # Perform the client or server part of the handshake.
        if isServer:
            self.__file.write('\0\0\0\0')
            self.__file.flush()
        else:
            zero = self.__file.read(size=4)
            if len(zero) == '\0\0\0\0':
                raise RuntimeError('Incorrect handshake')

    def __del__(self):
        if self.__active:
            self.deactivate()

    def getErrorHook(self):
        return self.__errorHook

    def setErrorHook(self, newValue):
        self.__errorHook = newValue

    errorHook = property(getErrorHook, setErrorHook)

    # receiving

    def receiveNotification(self):
        size = self.__file.read(size=4)
        if len(size) == 0:
            self.__logger.info("Received EOF")
            raise EOFError()
        if not (len(size) == 4):
            raise RuntimeError('Short read when receiving notification size '
                               '(size: %s)' % len(size))
        size = ord(size[0])      \
            | ord(size[1]) << 8  \
            | ord(size[2]) << 16 \
            | ord(size[3]) << 24
        self.__logger.debug('Receiving notification of size %d', size)
        notification = self.__file.read(size=size)
        if not (len(notification) == size):
            raise RuntimeError(
                'Short read when receiving notification payload')
        return notification

    @staticmethod
    def bufferToNotification(serialized):
        notification = Notification()
        notification.ParseFromString(serialized)
        return notification

    def doOneNotification(self):
        serialized = self.receiveNotification()
        notification = self.bufferToNotification(serialized)
        self.dispatch(notification)

    def receiveNotifications(self):
        while True:
            self.__logger.debug('Receiving notifications')
            try:
                self.doOneNotification()
            except EOFError, e:
                self.__logger.info("Received EOF while reading")
                if not self.__activeShutdown:
                    self.shutdown()
                break
            except Exception, e:
                self.__logger.warn('Receive error: %s', e)
                break

        if self.errorHook is not None:
            self.errorHook(e)
        return

    # sending

    def sendNotification(self, notification):
        size = len(notification)
        self.__logger.info('Sending notification of size %d', size)
        size = ''.join((chr(size & 0x000000ff),
                        chr((size & 0x0000ff00) >> 8),
                        chr((size & 0x00ff0000) >> 16),
                        chr((size & 0xff000000) >> 24)))
        with self.__lock:
            self.__file.write(size)
            self.__file.write(notification)
            self.__file.flush()

    @staticmethod
    def notificationToBuffer(notification):
        return notification.SerializeToString()

    def handle(self, notification):
        serialized = self.notificationToBuffer(notification)
        self.sendNotification(serialized)

    # state management

    def activate(self):
        if self.__active:
            raise RuntimeError('Trying to activate active connection')

        with self.__lock:

            self.__thread = threading.Thread(target=self.receiveNotifications)
            self.__thread.start()

            self.__active = True

    def shutdown(self):
        with self.__lock:
            self.__activeShutdown = True
            self.__socket.shutdown(socket.SHUT_WR)

    def deactivate(self):

        with self.__lock:

            if not self.__active:
                raise RuntimeError('Trying to deactivate inactive connection')

            self.__active = False

            # If necessary, close the socket, this will cause an exception
            # in the notification receiver thread (unless we run in the
            # context that thread).
            self.__logger.info('Closing socket')
            try:
                self.__file.close()
                self.__socket.close()
            except Exception, e:
                self.__logger.warn('Failed to close socket: %s', e)

    def waitForDeactivation(self):
        self.__logger.info('Joining thread')
        self.__thread.join()


class Bus(object):
    """
    Instances of this class provide access to a socket-based bus.

    It is transparent for clients (connectors) of this class whether
    is accessed by running the bus server or by connecting to the bus
    server as a client.

    In-direction connectors add themselves as event sinks using the
    :obj:`addConnector` method.

    Out-direction connectors submit events to the bus using the
    :obj:`handleOutgoing` method.

    .. codeauthor:: jmoringe
    """
    def __init__(self):
        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        self.__connections = []
        self.__connectors = []
        self.__lock = threading.RLock()

        self.__active = False

    def getLock(self):
        return self.__lock

    lock = property(getLock)

    def getConnections(self):
        """
        Returns:
            list:
                A list of all connections to the bus.
        """
        return self.__connections

    def addConnection(self, connection):
        """
        Add ``connection`` to the list of connections of this bus. This
        cause notifications send over this bus to be send through
        ``connection`` and notifications received via ``connection`` to
        be dispatched to connectors of this bus.

        Args:
            connection:
                The connection that should be added to this bus.
        """
        with self.lock:
            self.__connections.append(connection)

            class Handler(object):

                def __init__(_self):
                    _self.bus = self

                def __call__(_self, notification):
                    self.handleIncoming((connection, notification))
            connection.addHandler(Handler())

            def removeAndDeactivate(exception):
                self.removeConnection(connection)
                try:
                    connection.deactivate()
                except Exception, e:
                    self.__logger.warning(
                        "Error while deactivating connection %s: %s",
                        connection, e)
            connection.errorHook = removeAndDeactivate
            connection.activate()

    def removeConnection(self, connection):
        """
        Remove ``connection`` from the list of connections of this bus.

        Args:
            connection:
                The connection that should be removed from this bus.
        """
        self.__logger.info('Removing connection %s', connection)

        with self.lock:
            if connection in self.__connections:
                self.__connections.remove(connection)
                connection.removeHandler([h for h in connection.handlers
                                          if h.bus is self][0])

    connections = property(getConnections)

    def getConnectors(self):
        return self.__connectors

    def addConnector(self, connector):
        """
        Add ``connector`` to the list of connectors of this
        bus. Depending on the direction of ``connector``, this causes
        ``connector`` to either receive or broadcast notifications via
        this bus.

        Args:
            connector:
                The connector that should be added to this bus.
        """
        self.__logger.info('Adding connector %s', connector)
        with self.lock:
            self.__connectors.append(connector)

    def removeConnector(self, connector):
        """
        Remove ``connector`` from the list of connectors of this bus.

        Args:
            connector:
                The connector that should be removed from this bus.
        """
        self.__logger.info('Removing connector %s', connector)
        with self.lock:
            self.__connectors.remove(connector)
            if not self.__connectors:
                self.__logger.info(
                    'Removed last connector; requesting deletion')
                return False
            return True

    connectors = property(getConnectors)

    def handleIncoming(self, connectionAndNotification):
        _, notification = connectionAndNotification
        self.__logger.debug('Trying to distribute notification to connectors')
        with self.lock:
            self.__logger.debug(
                'Locked bus to distribute notification to connectors')
            if not self.__active:
                self.__logger.info(
                    'Cancelled distribution to connectors '
                    'since bus is not active')
                return

            # Distribute the notification to participants in our
            # process via InPushConnector instances.
            self._toConnectors(notification)

    def handleOutgoing(self, notification):
        with self.lock:
            self.__logger.debug('Locked bus to distribute notification to '
                                'connections and connectors')
            if not self.__active:
                self.__logger.info('Cancelled distribution to connections '
                                   'and connectors since bus is not active')
                return

            # Distribute the notification to remote participants via
            # network connections.
            failing = self._toConnections(notification)
            # Distribute the notification to participants in our own
            # process via InPushConnector instances.
            self._toConnectors(notification)
        # there are only failing connection in case of an unorderly shutdown.
        # So the shutdown protocol does not apply here and
        # we can immediately call deactivate.
        map(BusConnection.deactivate, failing)

    # State management

    def activate(self):
        if self.__active:
            raise RuntimeError('Trying to activate active bus')

        with self.lock:
            self.__active = True

    def deactivate(self):
        if not self.__active:
            raise RuntimeError('Trying to deactivate inactive bus')

        with self.lock:
            self.__active = False
            connectionsCopy = copy.copy(self.connections)

        # We do not have to lock the bus here, since
        # 1) removeConnection will do that for each connection
        # 2) the connection list will not be modified concurrently at
        #    this point
        self.__logger.info('Closing connections')
        for connection in connectionsCopy:
            try:
                self.removeConnection(connection)
                connection.shutdown()
                connection.waitForDeactivation()
            except Exception, e:
                self.__logger.error('Failed to close connections: %s', e)

    # Low-level helpers

    def _toConnections(self, notification, exclude=None):
        failing = []
        for connection in self.connections:
            if connection is not exclude:
                try:
                    connection.handle(notification)
                except Exception, e:
                    self.__logger.warn(
                        'Failed to send to %s: %s; '
                        'will close connection later',
                        connection, e)
                    failing.append(connection)

        # Removed connections for which sending the notification
        # failed.
        map(self.removeConnection, failing)
        return failing

    def _toConnectors(self, notification):
        # Deliver NOTIFICATION to connectors which fulfill two
        # criteria:
        # 1) Direction has to be "incoming events"
        # 2) The scope of the connector has to be a superscope of
        #    NOTIFICATION's scope
        scope = rsb.Scope(notification.scope)
        for connector in self.connectors:
            # TODO connector.direction == 'in' instead of isinstance
            if isinstance(connector, InPushConnector) \
               and (connector.scope == scope or
                    connector.scope.isSuperScopeOf(scope)):
                connector.handle(notification)

    def __repr__(self):
        return '<%s %d connection(s) %d connector(s) at 0x%x>' \
            % (type(self).__name__,
               len(self.getConnections()),
               len(self.getConnectors()),
               id(self))

__busClients = {}
__busClientsLock = threading.Lock()


def getBusClientFor(host, port, tcpnodelay, connector):
    """
    Return (creating it if necessary), a :obj:`BusClient` for the endpoint
    designated by ``host`` and ``port`` and attach ``connector`` to
    it. Attaching ``connector`` marks the bus client as being in use
    and protects it from being destroyed in a race condition
    situation.

    Args:
        host (str):
            A hostname or address of the node on which the bus server listens.
        port (int):
            The port on which the bus server listens.
        tcpnodelay (bool):
            If True, the socket will be set to TCP_NODELAY.
        connector:
            A connector that should be attached to the bus client.
    """
    key = (host, port, tcpnodelay)
    with __busClientsLock:
        bus = __busClients.get(key)
        if bus is None:
            bus = BusClient(host, port, tcpnodelay)
            __busClients[key] = bus
            bus.activate()
            bus.addConnector(connector)
        else:
            bus.addConnector(connector)
        return bus


class BusClient(Bus):
    """
    Instances of this class provide access to a bus by means of a
    client socket.

    .. codeauthor:: jmoringe
    """
    def __init__(self, host, port, tcpnodelay):
        """
        Args:
            host (str):
                A hostname or address of the node on which the bus server
                listens.
            port (int):
                The port on which the new bus server listens.
            tcpnodelay (bool):
                If True, the socket will be set to TCP_NODELAY.
        """
        super(BusClient, self).__init__()

        self.addConnection(BusConnection(host, port, tcpnodelay=tcpnodelay))

__busServers = {}
__busServersLock = threading.Lock()


def getBusServerFor(host, port, tcpnodelay, connector):
    """
    Return (creating it if necessary), a :obj:`BusServer` for the endpoint
    designated by ``host`` and ``port`` and attach ``connector`` to
    it. Attaching ``connector`` marks the bus server as being in use
    and protects it from being destroyed in a race condition
    situation.

    Args:
        host (str):
            A hostname or address identifying the interface to which the listen
            socket of the new bus server should be bound.
        port (int):
            The port to which the listen socket of the new bus server should be
            bound.
        tcpnodelay (bool):
            If True, the socket will be set to TCP_NODELAY.
        connector:
            A connector that should be attached to the bus server.
    """
    key = (host, port, tcpnodelay)
    with __busServersLock:
        bus = __busServers.get(key)
        if bus is None:
            bus = BusServer(host, port, tcpnodelay)
            bus.activate()
            __busServers[key] = bus
            bus.addConnector(connector)
        else:
            bus.addConnector(connector)
        return bus


class BusServer(Bus):
    """
    Instances of this class provide access to a socket-based bus for
    local and remote bus clients.

    Remote clients can connect to a server socket in order to send and
    receive events through the resulting socket connection.

    Local clients (connectors) use the usual :obj:`Bus` interface to
    receive events submitted by remote clients and submit events which
    will be distributed to remote clients by the :obj:`BusServer`.

    .. codeauthor:: jmoringe
    """

    def __init__(self, host, port, tcpnodelay, backlog=5):
        """
        Args:
            host (str):
                A hostname or address identifying the interface to which the
                listen socket of the new bus server should be bound.
            port (int):
                The port to which the listen socket of the new bus server
                should be bound.
            tcpnodelay (bool):
                If True, the socket will be set to TCP_NODELAY.
            backlog (int):
                The maximum number of queued connection attempts.
        """
        super(BusServer, self).__init__()

        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        self.__host = host
        self.__port = port
        self.__tcpnodelay = tcpnodelay
        self.__backlog = backlog
        self.__socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.__acceptorThread = None

        self.__active = False

    def __del__(self):
        if self.__active:
            self.deactivate()

    def acceptClients(self):
        import sys
        if sys.platform == 'darwin':
            self.__socket.settimeout(1.0)
        while self.__socket:
            self.__logger.info('Waiting for clients')
            try:
                clientSocket, addr = self.__socket.accept()
                if sys.platform == 'darwin':
                    clientSocket.settimeout(None)
                self.__logger.info('Accepted client %s', addr)
                self.addConnection(BusConnection(socket_=clientSocket,
                                                 isServer=True,
                                                 tcpnodelay=self.__tcpnodelay))
            except socket.timeout, e:
                if sys.platform != 'darwin':
                    self.__logger.error(
                        'Unexpected timeout in acceptClients: "%s"', e)
            except Exception, e:
                if self.__active:
                    self.__logger.error('Exception in acceptClients: "%s"', e,
                                        exc_info=True)
                else:
                    self.__logger.info('Acceptor thread terminating')

    # Receiving notifications

    def handleIncoming(self, connectionAndNotification):
        super(BusServer, self).handleIncoming(connectionAndNotification)

        # Distribute the notification to all connections except the
        # one that sent it.
        (sendingConnection, notification) = connectionAndNotification
        with self.lock:
            self._toConnections(notification, exclude=sendingConnection)

    # State management

    def activate(self):
        super(BusServer, self).activate()

        if self.__active:
            raise RuntimeError('Trying to activate active BusServer')

        # Bind the socket and start listening
        self.__logger.info('Opening listen socket %s:%d',
                           '0.0.0.0', self.__port)
        self.__socket.bind(('0.0.0.0', self.__port))
        self.__socket.listen(self.__backlog)

        self.__logger.info('Starting acceptor thread')
        self.__acceptorThread = threading.Thread(target=self.acceptClients)
        self.__acceptorThread.start()

        self.__active = True

    def deactivate(self):
        if not self.__active:
            raise RuntimeError('Trying to deactivate inactive BusServer')

        self.__active = False

        # If necessary, close the listening socket. This causes an
        # exception in the acceptor thread.
        self.__logger.info('Closing listen socket')
        if self.__socket is not None:
            try:
                self.__socket.shutdown(socket.SHUT_RDWR)
            except Exception, e:
                self.__logger.warn('Failed to shutdown listen socket: %s', e)
            try:
                self.__socket.close()
            except:
                self.__logger.warn('Failed to close listen socket: %s', e)
            self.__socket = None

        # The acceptor thread should encounter an exception and exit
        # eventually. We wait for that.
        self.__logger.info('Waiting for acceptor thread')
        if self.__acceptorThread is not None:
            self.__acceptorThread.join()

        super(BusServer, self).deactivate()


def removeConnector(bus, connector):
    def removeAndMaybeKill(lock, dictionary):
        with lock:
            if not bus.removeConnector(connector):
                bus.deactivate()
                del dictionary[[key for (key, value) in dictionary.items()
                                if value is bus][0]]

    if isinstance(bus, BusClient):
        removeAndMaybeKill(__busClientsLock, __busClients)
    else:
        removeAndMaybeKill(__busServersLock, __busServers)


class Connector(rsb.transport.Connector,
                rsb.transport.ConverterSelectingConnector):
    """
    Instances of subclasses of this class receive events from a bus
    (represented by a :obj:`Bus` object) that is accessed via a socket
    connection.

    .. codeauthor:: jmoringe
    """

    def __init__(self, converters, options=None, **kwargs):
        super(Connector, self).__init__(wireType=bytearray,
                                        converters=converters,
                                        **kwargs)
        self.__logger = rsb.util.getLoggerByClass(self.__class__)

        if options is None:
            options = {}

        self.__active = False

        self.__bus = None
        self.__host = options.get('host', 'localhost')
        self.__port = int(options.get('port', '55555'))
        self.__tcpnodelay = options.get('nodelay', '1') in ['1', 'true']
        serverString = options.get('server', 'auto')
        if serverString in ['1', 'true']:
            self.__server = True
        elif serverString in ['0', 'false']:
            self.__server = False
        elif serverString == 'auto':
            self.__server = 'auto'
        else:
            raise TypeError('Server option has to be '
                            '"1", "true", "0", "false" or "auto", not "%s"'
                            % serverString)

    def __del__(self):
        if self.__active:
            self.deactivate()

    def __getBus(self, host, port, tcpnodelay, server):
        self.__logger.info('Requested server role: %s', server)

        if server == True:
            self.__logger.info('Getting bus server %s:%d', host, port)
            self.__bus = getBusServerFor(host, port, tcpnodelay, self)
        elif server == False:
            self.__logger.info('Getting bus client %s:%d', host, port)
            self.__bus = getBusClientFor(host, port, tcpnodelay, self)
        elif server == 'auto':
            try:
                self.__logger.info(
                    'Trying to get bus server %s:%d (in server = auto mode)',
                    host, port)
                self.__bus = getBusServerFor(host, port, tcpnodelay, self)
            except Exception, e:
                self.__logger.info('Failed to get bus server: %s', e)
                self.__logger.info(
                    'Trying to get bus client %s:%d (in server = auto mode)',
                    host, port)
                self.__bus = getBusClientFor(host, port, tcpnodelay, self)
        else:
            raise TypeError(
                'Server argument has to be True, False or "auto", not "%s"'
                % server)
        self.__logger.info('Got %s', self.__bus)
        return self.__bus

    def getBus(self):
        return self.__bus

    bus = property(getBus)

    def activate(self):
        if self.__active:
            raise RuntimeError('Trying to activate active connector')

        self.__logger.info('Activating')

        self.__bus = self.__getBus(self.__host,
                                   self.__port,
                                   self.__tcpnodelay,
                                   self.__server)

        self.__active = True

    def deactivate(self):
        if not self.__active:
            raise RuntimeError('Trying to deactivate inactive connector')

        self.__logger.info('Deactivating')

        self.__active = False

        removeConnector(self.bus, self)

    def setQualityOfServiceSpec(self, qos):
        pass

    def getTransportURL(self):
        query = '?tcpnodelay=' + ('1' if self.__tcpnodelay else '0')
        return 'socket://' + self.__host + ':' + str(self.__port) + query


class InPushConnector(Connector,
                      rsb.transport.InConnector):
    """
    Instances of this class receive events from a bus (represented by
    a :obj:`Bus` object) that is accessed via a socket connection.

    The receiving and dispatching of events is done in push mode: each
    instance has a :obj:`Bus` which pushes appropriate events into the
    instance. The connector deserializes event payloads and pushes the
    events into handlers (usually objects which implement some event
    processing strategy).

    .. codeauthor:: jmoringe
    """

    def __init__(self, **kwargs):
        self.__action = None

        super(InPushConnector, self).__init__(**kwargs)

    def filterNotify(self, theFilter, action):
        pass

    def setObserverAction(self, action):
        self.__action = action

    def handle(self, notification):
        if self.__action is None:
            return

        converter = self.getConverterForWireSchema(notification.wire_schema)
        event = conversion.notificationToEvent(
            notification,
            wireData=bytearray(notification.data),
            wireSchema=notification.wire_schema,
            converter=converter)
        self.__action(event)


class OutConnector(Connector,
                   rsb.transport.OutConnector):
    """
    Instance of this class send events to a bus (represented by a
    :obj:`Bus` object) that is accessed via a socket connection.

    .. codeauthor:: jmoringe
    """

    def __init__(self, **kwargs):
        super(OutConnector, self).__init__(**kwargs)

    def handle(self, event):
        # Create a notification fragment for the event and send it
        # over the bus.
        event.getMetaData().setSendTime()
        converter = self.getConverterForDataType(event.type)
        wireData, wireSchema = converter.serialize(event.data)
        notification = Notification()
        conversion.eventToNotification(notification, event,
                                       wireSchema=wireSchema,
                                       data=wireData)
        self.bus.handleOutgoing(notification)


class TransportFactory(rsb.transport.TransportFactory):
    """
    :obj:`TransportFactory` implementation for the socket transport.

    .. codeauthor:: jwienke
    """

    def getName(self):
        return "socket"

    def isRemote(self):
        return True

    def createInPushConnector(self, converters, options):
        return InPushConnector(converters=converters, options=options)

    def createOutConnector(self, converters, options):
        return OutConnector(converters=converters, options=options)


def initialize():
    try:
        rsb.transport.registerTransport(TransportFactory())
    except ValueError:
        pass
