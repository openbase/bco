# ============================================================
#
# Copyright (C) 2014, 2015, 2016 Jan Moringen <jmoringe@techfak.uni-bielefeld.de>
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
This package contains partial introspection functionality for RSB.

The introspection functionality is implemented in terms of RSB events
and thus built on top of "ordinary" RSB communication.

This package implements the "local introspection" (i.e. introspection
sender) part of the introspection architecture.

.. codeauthor:: jmoringe
"""

import sys
import os
import platform
import getpass

import copy
import uuid

import rsb
import rsb.version
from rsb.util import getLoggerByClass
import rsb.converter

from rsb.protocol.introspection.Hello_pb2 import Hello
from rsb.protocol.introspection.Bye_pb2 import Bye

_displayName = None

# Model


class ParticipantInfo(object):
    """
    Instances of this class store information about a participant.

    The participant can reside in the current process or in a remote
    process.

    .. codeauthor:: jmoringe
    """

    def __init__(self, kind, id, scope, type, parentId=None,
                 transportURLs=None):
        self.__kind = kind
        self.__id = id
        self.__scope = rsb.Scope.ensureScope(scope)
        self.__type = type
        self.__parentId = parentId
        self.__transportURLs = transportURLs or []

    def getKind(self):
        """
        Returns the kind of the participant.

        Examples include "listener", "informer" and "local-server".

        Returns:
            str:
                A lower-case, hyphen-separated string identifying the kind of
                participant.
        """
        return self.__kind

    kind = property(getKind)

    def getId(self):
        """
        Returns the unique id of the participant.

        Returns:
            uuid.uuid:
                The unique id of the participant.
        """
        return self.__id

    id = property(getId)

    def getScope(self):
        """
        Returns the scope of the participant.

        Returns:
            rsb.Scope:
                The scope of the participant.
        """
        return self.__scope

    scope = property(getScope)

    def getType(self):
        """
        Returns a representation of the type of the participant, if
        available.

        Note that this is a temporary solution and will change in
        future versions.

        Returns:
            type or tuple:
                A representation of the type.
        """
        return self.__type

    type = property(getType)

    def getParentId(self):
        """
        Return the unique id of the parent participant of the participant,
        or ``None``, if the participant does not have a parent.

        Returns:
            uuid.uuid or NoneType:
                ``None`` or the unique id of the participant's parent.
        """
        return self.__parentId

    parentId = property(getParentId)

    def getTransportURLs(self):
        """
        Return list of transport URLs.

        Returns:
            list:
                List of transport URLs describing the transports used
                by the participant.
        """
        return self.__transportURLs

    transportURLs = property(getTransportURLs)

    def __str__(self):
        return '<%s %s %s at 0x%0x>' \
            % (type(self).__name__, self.kind, self.scope.toString(), id(self))

    def __repr__(self):
        return str(self)

__processStartTime = None


def processStartTime():
    """
    Return the start time of the current process (or an approximation)
    in fractional seconds since UNIX epoch.

    Returns:
        float:
            Start time in factional seconds since UNIX epoch.
    """
    global __processStartTime

    # Used cached value, if there is one.
    if __processStartTime is not None:
        return __processStartTime

    # Try to determine the start time of the current process in a
    # platform dependent way. Since all of these methods seem kind of
    # error prone, allow failing silently and fall back to the default
    # implementation below.
    if 'linux' in sys.platform:
        try:
            import re

            procStatContent = open('/proc/stat').read()
            btimeEntry = re.match('(?:.|\n)*btime ([0-9]+)',
                                  procStatContent).group(1)
            bootTimeUNIXSeconds = int(btimeEntry)

            selfStatContent = open('/proc/self/stat').read()
            startTimeBootJiffies = int(selfStatContent.split(' ')[21])

            __processStartTime = float(bootTimeUNIXSeconds) \
                + float(startTimeBootJiffies) / 100.0
        except:
            pass

    # Default/fallback strategy: just use the current time.
    if __processStartTime is None:
        import time
        __processStartTime = time.time()

    return __processStartTime


def programName():
    import __main__
    if hasattr(__main__, '__file__'):
        return __main__.__file__
    else:
        return '<no script>'


class ProcessInfo(object):
    """
    Instances of this class store information about operating system
    processes.

    The stored information can describe the current process, a
    different process on the local machine or a remote process.

    .. codeauthor:: jmoringe
    """

    def __init__(self,
                 id=os.getpid(),
                 programName='python%d.%d %s'
                 % (sys.version_info.major,
                    sys.version_info.minor,
                    programName()),
                 arguments=copy.copy(sys.argv),
                 startTime=processStartTime(),
                 executingUser=None,
                 rsbVersion=rsb.version.getVersion()):
        self.__id = id
        self.__programName = programName
        self.__arguments = arguments
        self.__startTime = startTime
        self.__executingUser = executingUser
        if not self.__executingUser:
            try:
                self.__executingUser = getpass.getuser()
            except OSError:
                pass
        self.__rsbVersion = rsbVersion

    def getId(self):
        """
        Returns the numeric id of the process.

        Returns:
            int:
                The numeric id of the process.
        """
        return self.__id

    id = property(getId)

    def getProgramName(self):
        """
        Returns the name of the program being executed in the process.

        Returns:
            str:
                The name of the program.
        """
        return self.__programName

    programName = property(getProgramName)

    def getArguments(self):
        """
        Returns the list of commandline argument the process has been
        started with.

        Returns:
            list:
                A list of commandline argument strings
        """
        return self.__arguments

    arguments = property(getArguments)

    def getStartTime(self):
        """
        Returns the start time of the process in fractional seconds
        since UNIX epoch.

        Returns:
            float:
                start time in fractional seconds since UNIX epoch.
        """
        return self.__startTime

    startTime = property(getStartTime)

    def getExecutingUser(self):
        """
        Return the login- or account-name of the user executing the
        process.

        Returns:
            str:
                login- or account-name of the user executing the process or
                None if not determinable
        """
        return self.__executingUser

    executingUser = property(getExecutingUser)

    def getRSBVersion(self):
        """
        Return the version of the RSB implementation used in this process.

        Returns:
            str:
                Version string of the form::

                   MAJOR.MINOR.REVISION[-COMMIT]
        """
        return self.__rsbVersion

    rsbVersion = property(getRSBVersion)

    def __str__(self):
        return '<%s %s [%d] at 0x%0x>' \
            % (type(self).__name__, self.programName, self.id, id(self))

    def __repr__(self):
        return str(self)


def hostId():
    """
    Returns a unique id string for the current host.

    Returns:
        str or NoneType:
            A platform-dependent, string (hopefully) uniquely identifying the
            current host or ``None`` if such an id cannot be obtained.
    """
    def maybeRead(filename):
        try:
            with open(filename, 'r') as f:
                return f.read().strip()
        except:
            return None

    return \
        ('linux' in sys.platform and maybeRead('/var/lib/dbus/machine-id')) \
        or ('linux' in sys.platform and maybeRead('/ect/machine-id')) \
        or None


def machineType():
    result = platform.machine().lower()
    if result in ['i368', 'i586', 'i686']:
        return 'x86'
    elif result in ['x86_64', 'amd64']:
        return 'x86_64'
    else:
        return result


def machineVersion():
    if 'linux' in sys.platform:
        import re

        try:
            cpuInfo = open('/proc/cpuinfo').read()
            return re.match('(?:.|\n)*model name\t: ([^\n]+)',
                            cpuInfo).group(1)
        except:
            return None


class HostInfo(object):
    """
    Instances of this class store information about a host.

    The stored information can describe the local host or a remote
    host.

    .. codeauthor:: jmoringe
    """

    def __init__(self,
                 id=hostId(),
                 hostname=platform.node().split('.')[0],
                 machineType=machineType(),
                 machineVersion=machineVersion(),
                 softwareType=platform.system().lower(),
                 softwareVersion=platform.release()):
        self.__id = id
        self.__hostname = hostname
        self.__machineType = machineType
        self.__machineVersion = machineVersion
        self.__softwareType = softwareType
        self.__softwareVersion = softwareVersion

    def getId(self):
        """
        Return the unique id string for the host.

        Returns:
            str or None:
                The platform-dependent, (hopefully) unique id string.
        """
        return self.__id

    id = property(getId)

    def getHostname(self):
        """
        Returns the hostname of the host.

        Returns:
            str:
                The hostname.
        """
        return self.__hostname

    hostname = property(getHostname)

    def getMachineType(self):
        """
        Return the type of the machine, usually CPU architecture.

        Returns:
            str or NoneType:
                The machine type when known.
        """
        return self.__machineType

    machineType = property(getMachineType)

    def getMachineVersion(self):
        """
        Returns the version of the machine within its type, usually
        the CPU identification string.

        Returns:
            str or NoneType:
                The machine version when known.
        """
        return self.__machineVersion

    machineVersion = property(getMachineVersion)

    def getSoftwareType(self):
        """
        Returns the type of the operating system running on the host,
        usually the kernel name.

        Returns:
            str or NoneType:
                The software type when known.
        """
        return self.__softwareType

    softwareType = property(getSoftwareType)

    def getSoftwareVersion(self):
        """
        Returns the version of the operating system within its type,
        usually the kernel version string.

        Returns:
            str or NoneType:
                The software version when known.
        """
        return self.__softwareVersion

    softwareVersion = property(getSoftwareVersion)

    def __str__(self):
        return '<%s %s %s %s at 0x%0x>' \
            % (type(self).__name__,
               self.hostname, self.machineType, self.softwareType,
               id(self))

    def __repr__(self):
        return str(self)

# IntrospectionSender

BASE_SCOPE = rsb.Scope('/__rsb/introspection/')
PARTICIPANTS_SCOPE = BASE_SCOPE.concat(rsb.Scope('/participants/'))
HOSTS_SCOPE = BASE_SCOPE.concat(rsb.Scope('/hosts/'))


def participantScope(participantId, baseScope=PARTICIPANTS_SCOPE):
    return baseScope.concat(rsb.Scope('/' + str(participantId)))


def processScope(hostId, processId, baseScope=HOSTS_SCOPE):
    return (baseScope
            .concat(rsb.Scope('/' + hostId))
            .concat(rsb.Scope('/' + processId)))


class IntrospectionSender(object):
    """
    Instances of this class (usually zero or one per process) send
    information about participants in the current process, the current
    process itself and the local host to receivers of introspection
    information.

    Instances need to be notified of created and destroyed
    participants via calls of the :obj:`addParticipant` and
    :obj:`removeParticipant` methods.

    .. codeauthor:: jmoringe
    """

    def __init__(self):
        self.__logger = getLoggerByClass(self.__class__)

        self.__participants = []

        self.__process = ProcessInfo()
        self.__host = HostInfo()

        self.__informer = rsb.createInformer(PARTICIPANTS_SCOPE)
        self.__listener = rsb.createListener(PARTICIPANTS_SCOPE)

        def handle(event):
            # TODO use filter when we get conjunction filter
            if event.method not in ['REQUEST', 'SURVEY']:
                return

            participantId = None
            participant = None
            if len(event.scope.components) > \
                    len(PARTICIPANTS_SCOPE.components):
                try:
                    participantId = uuid.UUID(event.scope.components[-1])
                    if participantId is not None:
                        # TODO there has to be a better way
                        for p in self.__participants:
                            if p.id == participantId:
                                participant = p
                            break
                except Exception, e:
                    self.__logger.warn('Query event %s does not '
                                       'properly address a participant: %s',
                                       event, e)

            def process(thunk):
                if participant is not None and event.method == 'REQUEST':
                    thunk(query=event, participant=participant)
                elif participant is None and event.method == 'SURVEY':
                    for p in self.__participants:
                        thunk(query=event, participant=p)
                else:
                    self.__logger.warn('Query event %s not understood', event)

            if event.data is None:
                process(self.sendHello)
            elif event.data == 'ping':
                process(self.sendPong)
            else:
                self.__logger.warn('Query event %s not understood', event)

        self.__listener.addHandler(handle)

        self.__server = rsb.createServer(
            processScope(self.__host.id or self.__host.hostname,
                         str(self.__process.id)))

        def echo(request):
            reply = rsb.Event(scope=request.scope,
                              data=request.data,
                              type=type(request.data))
            reply.metaData.setUserTime('request.send',
                                       request.metaData.sendTime)
            reply.metaData.setUserTime('request.receive',
                                       request.metaData.receiveTime)
            return reply
        self.__server.addMethod('echo', echo,
                                requestType=rsb.Event,
                                replyType=rsb.Event)

    def deactivate(self):
        self.__listener.deactivate()
        self.__informer.deactivate()
        self.__server.deactivate()

    def getProcess(self):
        return self.__process

    process = property(getProcess)

    def getHost(self):
        return self.__host

    host = property(getHost)

    def addParticipant(self, participant, parent=None):
        parentId = None
        if parent:
            parentId = parent.id

        def camelCaseToDashSeperated(name):
            result = []
            for i, c in enumerate(name):
                if c.isupper() and i > 0 and name[i - 1].islower():
                    result.append('-')
                result.append(c.lower())
            return ''.join(result)

        info = ParticipantInfo(
            kind=camelCaseToDashSeperated(type(participant).__name__),
            id=participant.id,
            parentId=parentId,
            scope=participant.scope,
            type=object,  # TODO
            transportURLs=participant.transportURLs)

        self.__participants.append(info)

        self.sendHello(info)

    def removeParticipant(self, participant):
        removed = None
        for p in self.__participants:
            if p.id == participant.id:
                removed = p
                break

        if removed is not None:
            self.__participants.remove(removed)
            self.sendBye(removed)

        return bool(self.__participants)

    def sendHello(self, participant, query=None):
        hello = Hello()
        hello.kind = participant.kind
        hello.id = participant.id.get_bytes()
        hello.scope = participant.scope.toString()
        if participant.parentId:
            hello.parent = participant.parentId.get_bytes()
        for url in participant.transportURLs:
            hello.transport.append(url)

        host = hello.host
        if self.host.id is None:
            host.id = self.host.hostname
        else:
            host.id = self.host.id
        host.hostname = self.host.hostname
        host.machine_type = self.host.machineType
        if self.host.machineVersion is not None:
            host.machine_version = self.host.machineVersion
        host.software_type = self.host.softwareType
        host.software_version = self.host.softwareVersion

        process = hello.process
        process.id = str(self.process.id)
        process.program_name = self.process.programName
        for argument in self.process.arguments:
            process.commandline_arguments.append(argument)
        process.start_time = int(self.process.startTime * 1000000.0)
        if self.process.executingUser:
            process.executing_user = self.process.executingUser
        process.rsb_version = self.process.rsbVersion
        if _displayName:
            process.display_name = _displayName
        scope = participantScope(participant.id, self.__informer.scope)
        helloEvent = rsb.Event(scope=scope,
                               data=hello,
                               type=type(hello))
        if query:
            helloEvent.addCause(query.id)
        self.__informer.publishEvent(helloEvent)

    def sendBye(self, participant):
        bye = Bye()
        bye.id = participant.id.get_bytes()

        scope = participantScope(participant.id, self.__informer.scope)
        byeEvent = rsb.Event(scope=scope,
                             data=bye,
                             type=type(bye))
        self.__informer.publishEvent(byeEvent)

    def sendPong(self, participant, query=None):
        scope = participantScope(participant.id, self.__informer.scope)
        pongEvent = rsb.Event(scope=scope,
                              data='pong',
                              type=str)
        if query:
            pongEvent.addCause(query.id)
        self.__informer.publishEvent(pongEvent)

__sender = None


def handleParticipantCreation(participant, parent=None):
    """
    This function is intended to be connected to
    :obj:`rsb.participantCreationHook` and calls
    :obj:`IntrospectionSender.addParticipant` when appropriate, first
    creating the :obj:`IntrospectionSender` instance, if necessary.
    """
    global __sender

    if participant.scope.isSubScopeOf(BASE_SCOPE) \
       or not participant.config.introspection:
        return

    if __sender is None:
        __sender = IntrospectionSender()
    __sender.addParticipant(participant, parent=parent)


def handleParticipantDestruction(participant):
    """
    This function is intended to be connected to
    :obj:`rsb.participantDestructionHook` and calls
    :obj:`IntrospectionSender.removeParticipant` when appropriate,
    potentially deleting the :obj:`IntrospectionSender` instance
    afterwards.
    """
    global __sender

    if participant.scope.isSubScopeOf(BASE_SCOPE) \
       or not participant.config.introspection:
        return

    if __sender and not __sender.removeParticipant(participant):
        __sender.deactivate()
        __sender = None


def initialize(displayName=None):
    """
    Initializes the introspection module. Clients need to ensure that this
    method is called only once.

    Args:
        displayName (str or NoneType if not set, optional):
            a user-defined process name to use in the introspection
    """
    global _displayName

    _displayName = displayName

    # Register converters for introspection messages
    for clazz in [Hello, Bye]:
        converter = rsb.converter.ProtocolBufferConverter(messageClass=clazz)
        rsb.converter.registerGlobalConverter(converter, replaceExisting=True)

    rsb.participantCreationHook.addHandler(handleParticipantCreation)
    rsb.participantDestructionHook.addHandler(handleParticipantDestruction)
