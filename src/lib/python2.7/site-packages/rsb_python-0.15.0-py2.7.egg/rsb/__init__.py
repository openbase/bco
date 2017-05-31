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
This package contains all classes that form the high-level user interface of
the RSB python implementation. It is the entry point for most users and only in
advanced cases client programs need to use classes from other modules.

In order to create basic objects have a look at the functions
:obj:`createInformer`, :obj:`createListener`, :obj:`createServer` and
:obj:`createRemoteServer`.

.. codeauthor:: jwienke
.. codeauthor:: jmoringe
"""

import uuid
import copy
import logging
import threading
import time
import re
import os
import platform
import ConfigParser

_logger = logging.getLogger('rsb')


# prevent logging warnings about missing handlers as per:
# https://docs.python.org/2.6/library/logging.html#configuring-logging-for-a-library
# do so before importing anything from RSB itself, which might already log
# stuff
class _NullHandler(logging.Handler):
    """
    Null logging handler to prevent warning messages
    """
    def emit(self, record):
        pass
_logger.addHandler(_NullHandler())

from rsb.util import getLoggerByClass, Enum
import rsb.eventprocessing
import rsb.filter

_spreadAvailable = False
try:
    import spread
    _spreadAvailable = True
except ImportError:
    pass


def haveSpread():
    """
    Indicates whether the installation of RSB has spread support.

    Returns:
        True if spread is available, else False
    """
    return _spreadAvailable

__defaultTransportsRegistered = False
__transportRegistrationLock = threading.RLock()


def __registerDefaultTransports():
    """
    Registers all available transports.
    """
    global __defaultTransportsRegistered
    with __transportRegistrationLock:
        if __defaultTransportsRegistered:
            return
        __defaultTransportsRegistered = True
        import rsb.transport.local as local
        local.initialize()
        import rsb.transport.socket as socket
        socket.initialize()
        if haveSpread():
            import rsb.transport.rsbspread as rsbspread
            rsbspread.initialize()


class QualityOfServiceSpec(object):
    """
    Specification of desired quality of service settings for sending
    and receiving events. Specification given here are required "at
    least". This means concrete connector implementations can provide
    "better" QoS specs without any notification to the clients. Better
    is decided by the integer value of the specification enums. Higher
    values mean better services.

    .. codeauthor:: jwienke
    """

    Ordering = Enum("Ordering", ["UNORDERED", "ORDERED"], [10, 20])
    Reliability = Enum("Reliability", ["UNRELIABLE", "RELIABLE"], [10, 20])

    def __init__(self, ordering=Ordering.UNORDERED,
                 reliability=Reliability.RELIABLE):
        """
        Constructs a new QoS specification with desired
        details. Defaults are unordered but reliable.

        Args:
            ordering:
                desired ordering type
            reliability:
                desired reliability type
        """
        self.__ordering = ordering
        self.__reliability = reliability

    def getOrdering(self):
        """
        Returns the desired ordering settings.

        Returns:
            ordering settings
        """

        return self.__ordering

    def setOrdering(self, ordering):
        """
        Sets the desired ordering settings

        Args:
            ordering: ordering to set
        """

        self.__ordering = ordering

    ordering = property(getOrdering, setOrdering)

    def getReliability(self):
        """
        Returns the desired reliability settings.

        Returns:
            reliability settings
        """

        return self.__reliability

    def setReliability(self, reliability):
        """
        Sets the desired reliability settings

        Args:
            reliability: reliability to set
        """

        self.__reliability = reliability

    reliability = property(getReliability, setReliability)

    def __eq__(self, other):
        try:
            return other.__reliability == self.__reliability \
                and other.__ordering == self.__ordering
        except (AttributeError, TypeError):
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __repr__(self):
        return "%s(%r, %r)" % (self.__class__.__name__,
                               self.__ordering,
                               self.__reliability)


CONFIG_DEBUG_VARIABLE = 'RSB_CONFIG_DEBUG'

CONFIG_FILES_VARIABLE = 'RSB_CONFIG_FILES'

CONFIG_FILE_KEY_SYSTEM = '%system'
CONFIG_FILE_KEY_PREFIX = '%prefix'
CONFIG_FILE_KEY_USER = '%user'
CONFIG_FILE_KEY_PWD = '%pwd'

DEFAULT_CONFIG_FILES = [CONFIG_FILE_KEY_SYSTEM,
                        CONFIG_FILE_KEY_PREFIX,
                        CONFIG_FILE_KEY_USER,
                        CONFIG_FILE_KEY_PWD]


def _configFileToDict(path, defaults=None):
    parser = ConfigParser.RawConfigParser()
    parser.read(path)
    if defaults is None:
        options = {}
    else:
        options = defaults
    for section in parser.sections():
        for (k, v) in parser.items(section):
            options[section + '.' + k] = v.split('#')[0].strip()
    return options


def _configEnvironmentToDict(defaults=None, debug=False):
    if defaults is None:
        options = {}
    else:
        options = defaults
    empty = True
    for (key, value) in os.environ.items():
        if key.startswith('RSB_'):
            if debug:
                empty = False
                print('     %s -> %s' % (key, value))
            if not key == CONFIG_FILES_VARIABLE and value == '':
                raise ValueError('The value of the environment variable '
                                 '%s is the empty string' % key)
            options[key[4:].lower().replace('_', '.')] = value
    if debug and empty:
        print('     <none>')
    return options


def _configDefaultConfigFiles():
    if CONFIG_FILES_VARIABLE in os.environ:
        return [f for f in os.environ[CONFIG_FILES_VARIABLE].split(':') if f]
    else:
        return DEFAULT_CONFIG_FILES


def _configDefaultSourcesToDict(defaults=None,
                                files=_configDefaultConfigFiles()):
    r"""
    Obtain configuration options from multiple sources, store them
    in a :obj:`ParticipantConfig` object and return it. By default,
    the following sources of configuration information will be
    consulted:

     1. ``/etc/rsb.conf``
     2. ``$prefix/etc/rsb.conf``
     3. ``~/.config/rsb.conf``
     4. ``\$(PWD)/rsb.conf``
     5. Environment Variables

    Args:
        defaults (dict of str -> str):
            dictionary with default options
        files (list of str)
            filenames and placeholders for configuration files

            The placeholders ``%system``, ``%prefix``, ``%user`` and
            ``%pwd`` can be used to refer to the sources 1-4 above.

    Returns:
        dict of str -> str:
            A dictionary object that contains the merged configuration options
            from the sources mentioned above.

    See Also:
        :obj:`_configFileToDict`, :obj:`_configEnvironmentToDict`:
    """

    # Prepare defaults.
    if defaults is None:
        defaults = {}
    if 'transport.socket.enabled' not in defaults:
        defaults['transport.socket.enabled'] = '1'
    if 'introspection.enabled' not in defaults:
        defaults['introspection.enabled'] = '1'
    if platform.system() == 'Windows':
        systemConfigFile = "c:\\rsb.conf"
    else:
        systemConfigFile = "/etc/rsb.conf"

    # Configure sources.
    debug = CONFIG_DEBUG_VARIABLE in os.environ

    fileIndex = [1]

    def fromFile(configFile, description):
        def processFile(partial):
            if debug:
                if fileIndex[0] == 1:
                    print('  1. Configuration files')
                print('     %d. %s "%s" %s'
                      % (fileIndex[0], description, configFile,
                         'exists' if os.path.exists(configFile)
                         else 'does not exist'))
                fileIndex[0] += 1
            return _configFileToDict(configFile, partial)
        return processFile

    def processEnvironment(partial):
        if debug:
            print('  2. Environment variables with prefix RSB_')
        return _configEnvironmentToDict(partial, debug=debug)

    def processSpec(spec):
        if spec == CONFIG_FILE_KEY_SYSTEM:
            return fromFile(systemConfigFile,
                            'System wide config file')
        elif spec == CONFIG_FILE_KEY_PREFIX:
            return fromFile('%s/etc/rsb.conf' % rsb.util.prefix(),
                            'Prefix wide config file')
        elif spec == CONFIG_FILE_KEY_USER:
            return fromFile(os.path.expanduser('~/.config/rsb.conf'),
                            'User config file')
        elif spec == CONFIG_FILE_KEY_PWD:
            return fromFile('rsb.conf', 'Current directory file')
        else:
            return fromFile(spec, 'User specified config file')
    sources = [processSpec(f) for f in files] + [processEnvironment]

    # Merge sources and defaults.
    if debug:
        print('Configuring with sources (lowest priority first)')
    return reduce(lambda partial, source: source(partial), sources, defaults)

_CONFIG_TRUE_VALUES = ['1', 'true', 'yes']


def _configValueIsTrue(value):
    return value in _CONFIG_TRUE_VALUES


class ParticipantConfig(object):
    """
    Objects of this class describe desired configurations for newly
    created :obj:`Participant` instances with respect to:

    * Quality of service settings
    * Error handling strategies (not currently used)
    * Employed transport mechanisms

      * Their configurations (e.g. port numbers)
      * Associated converters

    * Whether introspection should be enabled for the participant
      (enabled by default)

    .. codeauthor:: jmoringe
    """

    class Transport(object):
        """
        Objects of this class describe configurations of transports
        connectors. These consist of

        * Transport name
        * Enabled vs. Disabled
        * Optional converter selection
        * Transport-specific options

        .. codeauthor:: jmoringe
        """
        def __init__(self, name, options=None, converters=None):
            self.__name = name
            self.__enabled = _configValueIsTrue(options.get('enabled', '0'))

            # Extract freestyle options for the transport.
            if options is None:
                self.__options = {}
            else:
                self.__options = dict([(key, value)
                                       for (key, value) in options.items()
                                       if '.' not in key and
                                       not key == 'enabled'])
            # Find converter selection rules
            self.__converters = converters
            self.__converterRules = dict(
                [(key[len("converter.python."):], value)
                 for (key, value) in options.items()
                 if key.startswith('converter.python')])

        def getName(self):
            return self.__name

        name = property(getName)

        def isEnabled(self):
            return self.__enabled

        def setEnabled(self, flag):
            self.__enabled = flag

        enabled = property(isEnabled, setEnabled)

        def getConverters(self):
            return self.__converters

        def setConverters(self, converters):
            self.__converters = converters

        converters = property(getConverters, setConverters)

        def getConverterRules(self):
            return self.__converterRules

        def setConverterRules(self, converterRules):
            self.__converterRules = converterRules

        converterRules = property(getConverterRules, setConverterRules)

        def getOptions(self):
            return self.__options

        options = property(getOptions)

        def __deepcopy__(self, memo):
            result = copy.copy(self)
            result.__converters = copy.deepcopy(self.__converters, memo)
            result.__converterRules = copy.deepcopy(
                self.__converterRules, memo)
            result.__options = copy.deepcopy(self.__options, memo)
            return result

        def __str__(self):
            return ('ParticipantConfig.Transport[%s, enabled = %s, '
                    'converters = %s, converterRules = %s, options = %s]'
                    % (self.__name, self.__enabled, self.__converters,
                       self.__converterRules, self.__options))

        def __repr__(self):
            return str(self)

    def __init__(self,
                 transports=None,
                 options=None,
                 qos=None,
                 introspection=False):
        if transports is None:
            self.__transports = {}
        else:
            self.__transports = transports

        if options is None:
            self.__options = {}
        else:
            self.__options = options

        if qos is None:
            self.__qos = QualityOfServiceSpec()
        else:
            self.__qos = qos

        self.__introspection = introspection

    def getTransports(self, includeDisabled=False):
        return [t for t in self.__transports.values()
                if includeDisabled or t.isEnabled()]

    transports = property(getTransports)

    def getTransport(self, name):
        return self.__transports[name]

    def getQualityOfServiceSpec(self):
        return self.__qos

    def setQualityOfServiceSpec(self, newValue):
        self.__qos = newValue

    qualityOfServiceSpec = property(getQualityOfServiceSpec,
                                    setQualityOfServiceSpec)

    def getIntrospection(self):
        return self.__introspection

    def setIntrospection(self, newValue):
        self.__introspection = newValue

    introspection = property(getIntrospection, setIntrospection)

    def __deepcopy__(self, memo):
        result = copy.copy(self)
        result.__transports = copy.deepcopy(self.__transports, memo)
        result.__options = copy.deepcopy(self.__options, memo)
        return result

    def __str__(self):
        return 'ParticipantConfig[%s, options = %s, ' \
               'qos = %s, introspection = %s]' \
               % (self.__transports.values(), self.__options, self.__qos,
                  self.__introspection)

    def __repr__(self):
        return str(self)

    @classmethod
    def __fromDict(cls, options):
        def sectionOptions(section):
            return [(key[len(section) + 1:], value)
                    for (key, value) in options.items()
                    if key.startswith(section)]
        result = ParticipantConfig()

        # Quality of service
        qosOptions = dict(sectionOptions('qualityofservice'))
        result.__qos.setReliability(
            QualityOfServiceSpec.Reliability.fromString(
                qosOptions.get(
                    'reliability',
                    QualityOfServiceSpec().getReliability().__str__())))
        result.__qos.setOrdering(
            QualityOfServiceSpec.Ordering.fromString(
                qosOptions.get(
                    'ordering',
                    QualityOfServiceSpec().getOrdering().__str__())))

        # Transport options
        for transport in ['spread', 'socket', 'inprocess']:
            transportOptions = dict(sectionOptions('transport.%s' % transport))
            if transportOptions:
                result.__transports[transport] = cls.Transport(
                    transport, transportOptions)

        # Introspection options
        introspectionOptions = dict(sectionOptions('introspection'))
        result.__introspection = _configValueIsTrue(
            introspectionOptions.get('enabled', '1'))

        return result

    @classmethod
    def fromDict(cls, options):
        return cls.__fromDict(options)

    @classmethod
    def fromFile(cls, path, defaults=None):
        """
        Obtain configuration options from the configuration file
        ``path``, store them in a :obj:`ParticipantConfig` object and
        return it.

        A simple configuration file may look like this::

            [transport.spread]
            host = azurit # default type is string
            port = 5301 # types can be specified in angle brackets
            # A comment

        Args:
            path:
                File of path
            defaults (dict of str -> str):
                dictionary with default options

        Returns:
            ParticipantConfig:
                A new :obj:`ParticipantConfig` object containing the options
                read from ``path``.


        See Also:
            :obj:`fromEnvironment`, :obj:`fromDefaultSources`
        """
        return cls.__fromDict(_configFileToDict(path, defaults))

    @classmethod
    def fromEnvironment(cls, defaults=None):
        """
        Obtain configuration options from environment variables, store
        them in a :obj:`ParticipantConfig` object and return
        it. Environment variable names are mapped to RSB option names
        as illustrated in the following example::

           RSB_TRANSPORT_SPREAD_PORT -> transport spread port

        Args:
            defaults (dict of str -> str):
                dictionary with default options

        Returns:
            ParticipantConfig:
                :obj:`ParticipantConfig` object that contains the merged
                configuration options from ``defaults`` and relevant
                environment variables.

        See Also:
            :obj:`fromFile`, :obj:`fromDefaultSources`
        """
        return cls.__fromDict(_configEnvironmentToDict(defaults))

    @classmethod
    def fromDefaultSources(cls, defaults=None):
        r"""
        Obtain configuration options from multiple sources, store them
        in a :obj:`ParticipantConfig` object and return it. The following
        sources of configuration information will be consulted:

         1. ``/etc/rsb.conf``
         2. ``$prefix/etc/rsb.conf``
         3. ``~/.config/rsb.conf``
         4. ``$(PWD)/rsb.conf``
         5. Environment Variables

        Args:
            defaults (dict of str -> str):
                dictionary with default options

        Returns:
            ParticipantConfig:
                A :obj:`ParticipantConfig` object that contains the merged
                configuration options from the sources mentioned above.

        See Also:
            :obj:`fromFile`, :obj:`fromEnvironment`
        """

        return cls.__fromDict(_configDefaultSourcesToDict(defaults))


def convertersFromTransportConfig(transport):
    """
    Returns an object implementing the
    :obj:`rsb.converter.ConverterSelectionStrategy` protocol suitable for
    ``transport``.

    If ``transport.converters`` is not ``None``, it is used
    unmodified. Otherwise the specification in
    ``transport.converterRules`` is used.

    Returns:
        ConverterSelectionStrategy:
            The constructed ConverterSelectionStrategy object.

    """

    # There are two possible ways to configure converters:
    # 1) transport.converters: this is either None or an object
    #    implementing the "ConverterSelectionStrategy protocol"
    # 2) when transport.converters is None, transport.converterRules
    #    is used to construct an object implementing the
    #    "ConverterSelectionStrategy protocol"
    if transport.converters is not None:
        return transport.converters

    # Obtain a consistent converter set for the wire-type of
    # the transport:
    # 1. Find global converter map for the wire-type
    # 2. Find configuration options that specify converters
    #    for the transport
    # 3. Add converters from the global map to the unambiguous map of
    #    the transport, resolving conflicts based on configuration
    #    options when necessary
    # TODO hack!
    wireType = bytearray

    import rsb
    import rsb.converter
    converterMap = rsb.converter.UnambiguousConverterMap(wireType)
    # Try to add converters form global map
    globalMap = rsb.converter.getGlobalConverterMap(wireType)
    for ((wireSchema, dataType), converter) \
            in globalMap.getConverters().items():
        # Converter can be added if converterOptions does not
        # contain a disambiguation that gives precedence to a
        # different converter. map may still raise an
        # exception in case of ambiguity.
        if wireSchema not in transport.converterRules \
           or dataType.__name__ == transport.converterRules[wireSchema]:
            converterMap.addConverter(converter)
    return converterMap


class Scope(object):
    """
    A scope defines a channel of the hierarchical unified bus covered by RSB.
    It is defined by a surface syntax like ``"/a/deep/scope"``.

    .. codeauthor:: jwienke
    """

    __COMPONENT_SEPARATOR = "/"
    __COMPONENT_REGEX = re.compile("^[-_a-zA-Z0-9]+$")

    @classmethod
    def ensureScope(cls, thing):
        if isinstance(thing, cls):
            return thing
        else:
            return Scope(thing)

    def __init__(self, stringRep):
        """
        Parses a scope from a string representation.

        Args:
            stringRep (str or unicode):
                string representation of the scope
        Raises:
            ValueError:
                if ``stringRep`` does not have the right syntax
        """

        if len(stringRep) == 0:
            raise ValueError("The empty string does not designate a "
                             "scope; Use '/' to designate the root scope.")

        if isinstance(stringRep, unicode):
            try:
                stringRep = stringRep.encode('ASCII')
            except UnicodeEncodeError, e:
                raise ValueError('Scope strings have be encodable as '
                                 'ASCII-strings, but the supplied scope '
                                 'string cannot be encoded as ASCII-string: %s'
                                 % e)

        # append missing trailing slash
        if stringRep[-1] != self.__COMPONENT_SEPARATOR:
            stringRep += self.__COMPONENT_SEPARATOR

        rawComponents = stringRep.split(self.__COMPONENT_SEPARATOR)
        if len(rawComponents) < 1:
            raise ValueError("Empty scope is not allowed.")
        if len(rawComponents[0]) != 0:
            raise ValueError("Scope must start with a slash. "
                             "Given was '%s'." % stringRep)
        if len(rawComponents[-1]) != 0:
            raise ValueError("Scope must end with a slash. "
                             "Given was '%s'." % stringRep)

        self.__components = rawComponents[1:-1]

        for com in self.__components:
            if not self.__COMPONENT_REGEX.match(com):
                raise ValueError("Invalid character in component %s. "
                                 "Given was scope '%s'." % (com, stringRep))

    def getComponents(self):
        """
        Returns all components of the scope as an ordered list. Components are
        the names between the separator character '/'. The first entry in the
        list is the highest level of hierarchy. The scope '/' returns an empty
        list.

        Returns:
            list:
                components of the represented scope as ordered list with
                highest level as first entry
        """
        return copy.copy(self.__components)

    components = property(getComponents)

    def toString(self):
        """
        Reconstructs a fully formal string representation of the scope with
        leading an trailing slashes.

        Returns:
            str:
                string representation of the scope
        """

        string = self.__COMPONENT_SEPARATOR
        for com in self.__components:
            string += com
            string += self.__COMPONENT_SEPARATOR
        return string

    def concat(self, childScope):
        """
        Creates a new scope that is a sub-scope of this one with the
        subordinated scope described by the given
        argument. E.g. ``"/this/is/".concat("/a/test/")`` results in
        ``"/this/is/a/test"``.

        Args:
            childScope (Scope):
                child to concatenate to the current scope for forming a
                sub-scope

        Returns:
            Scope:
                new scope instance representing the created sub-scope
        """
        newScope = Scope("/")
        newScope.__components = copy.copy(self.__components)
        newScope.__components += childScope.__components
        return newScope

    def isSubScopeOf(self, other):
        """
        Tests whether this scope is a sub-scope of the given other scope, which
        means that the other scope is a prefix of this scope. E.g. "/a/b/" is a
        sub-scope of "/a/".

        Args:
            other (Scope):
                other scope to test

        Returns:
            Bool:
                ``True`` if this is a sub-scope of the other scope, equality
                gives ``False``, too
        """

        if len(self.__components) <= len(other.__components):
            return False

        return other.__components == \
            self.__components[:len(other.__components)]

    def isSuperScopeOf(self, other):
        """
        Inverse operation of :obj:`isSubScopeOf`.

        Args:
            other (Scope):
                other scope to test

        Returns:
            Bool:
                ``True`` if this scope is a strict super scope of the other
                scope. Equality also gives ``False``.

        """

        if len(self.__components) >= len(other.__components):
            return False

        return self.__components == other.__components[:len(self.__components)]

    def superScopes(self, includeSelf=False):
        """
        Generates all super scopes of this scope including the root
        scope "/".  The returned list of scopes is ordered by
        hierarchy with "/" being the first entry.

        Args:
            includeSelf (Bool):
                if set to ``True``, this scope is also included as last element
                of the returned list

        Returns:
            list of Scopes:
                list of all super scopes ordered by hierarchy, "/" being first
        """

        supers = []

        maxIndex = len(self.__components)
        if not includeSelf:
            maxIndex -= 1
        for i in range(maxIndex + 1):
            superScope = Scope("/")
            superScope.__components = self.__components[:i]
            supers.append(superScope)

        return supers

    def __eq__(self, other):
        if not isinstance(other, self.__class__):
            return False
        return self.__components == other.__components

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.toString())

    def __lt__(self, other):
        return self.toString() < other.toString()

    def __le__(self, other):
        return self.toString() <= other.toString()

    def __gt__(self, other):
        return self.toString() > other.toString()

    def __ge__(self, other):
        return self.toString() >= other.toString()

    def __str__(self):
        return "Scope[%s]" % self.toString()

    def __repr__(self):
        return '%s("%s")' % (self.__class__.__name__, self.toString())


class MetaData(object):
    """
    Objects of this class store RSB-specific and user-supplied
    meta-data items such as timing information.

    .. codeauthor:: jmoringe
    """
    def __init__(self,
                 createTime=None, sendTime=None,
                 receiveTime=None, deliverTime=None,
                 userTimes=None, userInfos=None):
        """
        Constructs a new :obj:`MetaData` object.

        Args:
            createTime:
                A timestamp designating the time at which the associated event
                was created.
            sendTime:
                A timestamp designating the time at which the associated event
                was sent onto the bus.
            receiveTime:
                A timestamp designating the time at which the associated event
                was received from the bus.
            deliverTime:
                A timestamp designating the time at which the associated event
                was delivered to the user-level handler by RSB.
            userTimes (dict of str -> float):
                A dictionary of user-supplied timestamps. dict from string name
                to double value as seconds since unix epoche
            userInfos (dict of str -> str):
                A dictionary of user-supplied meta-data items.
        """
        if createTime is None:
            self.__createTime = time.time()
        else:
            self.__createTime = createTime
        self.__sendTime = sendTime
        self.__receiveTime = receiveTime
        self.__deliverTime = deliverTime
        if userTimes is None:
            self.__userTimes = {}
        else:
            self.__userTimes = userTimes
        if userInfos is None:
            self.__userInfos = {}
        else:
            self.__userInfos = userInfos

    def getCreateTime(self):
        return self.__createTime

    def setCreateTime(self, createTime=None):
        if createTime is None:
            self.__createTime = time.time()
        else:
            self.__createTime = createTime

    createTime = property(getCreateTime, setCreateTime)

    def getSendTime(self):
        return self.__sendTime

    def setSendTime(self, sendTime=None):
        if sendTime is None:
            self.__sendTime = time.time()
        else:
            self.__sendTime = sendTime

    sendTime = property(getSendTime, setSendTime)

    def getReceiveTime(self):
        return self.__receiveTime

    def setReceiveTime(self, receiveTime=None):
        if receiveTime is None:
            self.__receiveTime = time.time()
        else:
            self.__receiveTime = receiveTime

    receiveTime = property(getReceiveTime, setReceiveTime)

    def getDeliverTime(self):
        return self.__deliverTime

    def setDeliverTime(self, deliverTime=None):
        if deliverTime is None:
            self.__deliverTime = time.time()
        else:
            self.__deliverTime = deliverTime

    deliverTime = property(getDeliverTime, setDeliverTime)

    def getUserTimes(self):
        return self.__userTimes

    def setUserTimes(self, userTimes):
        self.__userTimes = userTimes

    def setUserTime(self, key, timestamp=None):
        if timestamp is None:
            self.__userTimes[key] = time.time()
        else:
            self.__userTimes[key] = timestamp

    userTimes = property(getUserTimes, setUserTimes)

    def getUserInfos(self):
        return self.__userInfos

    def setUserInfos(self, userInfos):
        self.__userInfos = userInfos

    def setUserInfo(self, key, value):
        self.__userInfos[key] = value

    userInfos = property(getUserInfos, setUserInfos)

    def __eq__(self, other):
        try:
            return (self.__createTime == other.__createTime) and \
                (self.__sendTime == other.__sendTime) and \
                (self.__receiveTime == other.__receiveTime) and \
                (self.__deliverTime == other.__deliverTime) and \
                (self.__userInfos == other.__userInfos) and \
                (self.__userTimes == other.__userTimes)
        except (TypeError, AttributeError):
            return False

    def __neq__(self, other):
        return not self.__eq__(other)

    def __str__(self):
        return ('%s[createTime= %s, sendTime = %s, receiveTime = %s, '
                'deliverTime = %s, userTimes = %s, userInfos = %s]'
                % ('MetaData',
                   self.__createTime, self.__sendTime, self.__receiveTime,
                   self.__deliverTime, self.__userTimes, self.__userInfos))

    def __repr__(self):
        return self.__str__()


class EventId(object):
    """
    Uniquely identifies an Event by the sending participants ID and a sequence
    number within this participant. Optional conversion to uuid is possible.

    .. codeauthor:: jwienke
    """

    def __init__(self, participantId, sequenceNumber):
        self.__participantId = participantId
        self.__sequenceNumber = sequenceNumber
        self.__id = None

    def getParticipantId(self):
        """
        Return the sender id of this id.

        Returns:
            uuid.UUID:
                sender id
        """
        return self.__participantId

    def setParticipantId(self, participantId):
        """
        Sets the participant id of this event.

        Args:
            participantId (uuid.UUID):
                sender id to set.
        """
        self.__participantId = participantId

    participantId = property(getParticipantId, setParticipantId)

    def getSequenceNumber(self):
        """
        Return the sequence number of this id.

        Returns:
            int: sequence number of the id.
        """
        return self.__sequenceNumber

    def setSequenceNumber(self, sequenceNumber):
        """
        Sets the sequence number of this id.

        Args:
            sequenceNumber (int):
                new sequence number of the id.
        """
        self.__sequenceNumber = sequenceNumber

    sequenceNumber = property(getSequenceNumber, setSequenceNumber)

    def getAsUUID(self):
        """
        Returns a UUID encoded version of this id.

        Returns:
            uuid.uuid:
                id of the event as UUID
        """

        if self.__id is None:
            self.__id = uuid.uuid5(self.__participantId,
                                   '%08x' % self.__sequenceNumber)
        return self.__id

    def __eq__(self, other):
        try:
            return (self.__sequenceNumber == other.__sequenceNumber) and \
                (self.__participantId == other.__participantId)
        except (TypeError, AttributeError):
            return False

    def __neq__(self, other):
        return not self.__eq__(other)

    def __repr__(self):
        return "EventId(%r, %r)" % (self.__participantId,
                                    self.__sequenceNumber)

    def __hash__(self):
        prime = 31
        result = 1
        result = prime * result + hash(self.__participantId)
        result = prime * result + \
            (self.__sequenceNumber ^ (self.__sequenceNumber >> 32))
        return result


class Event(object):
    """
    Basic event class.

    Events are often caused by other events, which e.g. means that their
    contained payload was calculated on the payload of one or more other
    events.

    To express these relations each event contains a set of EventIds that
    express the direct causes of the event. This means, transitive event causes
    are not modeled.

    Cause handling is inspired by the ideas proposed in: David Luckham, The
    Power of Events, Addison-Wessley, 2007

    .. codeauthor:: jwienke
    """

    def __init__(self, id=None, scope=Scope("/"), method=None,
                 data=None, type=object,
                 metaData=None, userInfos=None, userTimes=None, causes=None):
        """
        Constructs a new event with undefined type, root scope and no data.

        Args:
            id (EventId):
                The id of this event
            scope (Scope or accepted by Scope constructor):
                A :obj:`Scope` designating the channel on which the event will
                be published.
            method (str):
                A string designating the "method category" which identifies the
                role of the event in some communication patters. Examples are
                ``"REQUEST"`` and ``"REPLY"``.
            data:
                data contained in this event
            type (types.TypeType):
                python data type of the contained data
            metaData (MetaData):
                meta data to use for the new event
            userInfos (dict of str -> str):
                key-value like store of user infos to add to the meta data of
                this event
            userTimes (dict of str -> str):
                additional timestamps to add to the meta data. dict from string
                timestamp name to value of timestamp as dobule of seconds unix
                epoch
            causes (list):
                A list of :obj:`EventId` instances of events which causes the
                newly constructed events.
        """

        self.__id = id
        self.__scope = Scope.ensureScope(scope)
        self.__method = method
        self.__data = data
        if type is None:
            raise ValueError("Type must not be None")
        self.__type = type
        if metaData is None:
            self.__metaData = MetaData()
        else:
            self.__metaData = metaData
        if userInfos is not None:
            for (key, value) in userInfos.items():
                self.__metaData.getUserInfos()[key] = value
        if userTimes is not None:
            for (key, value) in userTimes.items():
                self.__metaData.getUserTimes()[key] = value
        if causes is not None:
            self.__causes = copy.copy(causes)
        else:
            self.__causes = []

    def getSequenceNumber(self):
        """
        Return the sequence number of this event.

        .. deprecated:: 0.13
           use :meth:`getId` instead

        Returns:
            int:
                sequence number of the event.
        """
        return self.getId().getSequenceNumber()

    sequenceNumber = property(getSequenceNumber)

    def getId(self):
        """
        Returns the id of this event.

        Returns:
            int:
                id of the event

        Raises:
            RuntimeError:
                if the event does not have an id so far
        """

        if self.__id is None:
            raise RuntimeError("The event does not have an ID so far.")
        return self.__id

    def setId(self, theId):
        self.__id = theId

    id = property(getId, setId)

    def getScope(self):
        """
        Returns the scope of this event.

        Returns:
            Scope:
                scope
        """

        return self.__scope

    def setScope(self, scope):
        """
        Sets the scope of this event.

        Args:
            scope (Scope):
                scope to set
        """

        self.__scope = scope

    scope = property(getScope, setScope)

    def getSenderId(self):
        """
        Return the sender id of this event.

        .. deprecated:: 0.13

           use :func:`getId` instead

        Returns:
            uuid.UUID:
                sender id
        """
        return self.getId().getParticipantId()

    senderId = property(getSenderId)

    def getMethod(self):
        """
        Return the method of this event.

        Returns:
            str:
                A string designating the method of this event of ``None`` if
                this event does not have a method.
        """
        return self.__method

    def setMethod(self, method):
        """
        Sets the method of this event.

        Args:
            method (str):
                The new method. ``None`` is allowed.
        """
        self.__method = method

    method = property(getMethod, setMethod)

    def getData(self):
        """
        Returns the user data of this event.

        Returns:
            user data
        """

        return self.__data

    def setData(self, data):
        """
        Sets the user data of this event

        Args:
            data:
                user data
        """

        self.__data = data

    data = property(getData, setData)

    def getType(self):
        """
        Returns the type of the user data of this event.

        Returns:
            user data type

        """

        return self.__type

    def setType(self, theType):
        """
        Sets the type of the user data of this event

        Args:
            theType:
                user data type
        """

        self.__type = theType

    type = property(getType, setType)

    def getMetaData(self):
        return self.__metaData

    def setMetaData(self, metaData):
        self.__metaData = metaData

    metaData = property(getMetaData, setMetaData)

    def addCause(self, theId):
        """
        Adds a causing EventId to the causes of this event.

        Args:
            theId (EventId):
                id to add

        Returns:
            bool:
                True if the id was newly added, else False
        """
        if theId in self.__causes:
            return False
        else:
            self.__causes.append(theId)
            return True

    def removeCause(self, theId):
        """
        Removes a causing EventId from the causes of this event.

        Args:
            theId (EventId):
                id to remove

        Returns:
            bool:
                True if the id was remove, else False (because it did not
                exist)
        """
        if theId in self.__causes:
            self.__causes.remove(theId)
            return True
        else:
            return False

    def isCause(self, theId):
        """
        Checks whether a given id of an event is marked as a cause for this
        event.

        Args:
            theId (EventId):
                id to check

        Returns:
            bool:
                True if the id is a cause of this event, else False
        """
        return theId in self.__causes

    def getCauses(self):
        """
        Returns all causes of this event.

        Returns:
            list of EventIds:
                causing event ids
        """
        return self.__causes

    def setCauses(self, causes):
        """
        Overwrites the cause vector of this event with the given one.

        Args:
            causes (list of EventId):
                new cause vector
        """
        self.__causes = causes

    causes = property(getCauses, setCauses)

    def __str__(self):
        printData = str(self.__data)
        if len(printData) > 100:
            printData = printData[:100] + '...'
        printData = ''.join(['\\x%x' % ord(c)
                             if ord(c) < 32 else c for c in printData])
        return "%s[id = %s, scope = '%s', data = '%s', type = '%s', " \
            "method = '%s', metaData = %s, causes = %s]" \
            % ("Event", self.__id, self.__scope, printData, self.__type,
               self.__method, self.__metaData, self.__causes)

    def __repr__(self):
        return self.__str__()

    def __eq__(self, other):
        try:
            return (self.__id == other.__id) and \
                (self.__scope == other.__scope) and \
                (self.__type == other.__type) and \
                (self.__data == other.__data) and \
                (self.__metaData == other.__metaData) and \
                (self.__causes == other.__causes)
        except (TypeError, AttributeError):
            return False

    def __neq__(self, other):
        return not self.__eq__(other)


class Hook(object):
    """
    A mutable collection of callback functions that can be called
    together.

    .. codeauthor:: jmoringe
    """

    def __init__(self):
        self.__lock = threading.RLock()
        self.__handlers = []

    def run(self, *args, **kwargs):
        with self.__lock:
            for handler in self.__handlers:
                handler(*args, **kwargs)

    def addHandler(self, handler):
        with self.__lock:
            self.__handlers.append(handler)

    def removeHandler(self, handler):
        with self.__lock:
            self.__handlers.remove(handler)

participantCreationHook = Hook()

participantDestructionHook = Hook()


class Participant(object):
    """
    Base class for specialized bus participant classes. Has a unique
    id and a scope.

    .. codeauthor:: jmoringe
    """
    def __init__(self, scope, config):
        """
        Constructs a new Participant. This should not be done by
        clients.

        Args:
            scope (Scope or accepted by Scope constructor):
                scope of the bus channel.
            config (ParticipantConfig):
                Configuration that the participant should use

        See Also:
            :obj:`createListener`, :obj:`createInformer`, :obj:`createServer`,
            :obj:`createRemoteServer`
        """
        self.__id = uuid.uuid4()
        self.__scope = Scope.ensureScope(scope)
        self.__config = config

    def getId(self):
        return self.__id

    def setId(self, theId):
        self.__id = theId

    id = property(getId, setId)

    def getScope(self):
        return self.__scope

    def setScope(self, scope):
        self.__scope = scope

    scope = property(getScope, setScope)

    def getConfig(self):
        return self.__config

    config = property(getConfig)

    def getTransportURLs(self):
        """
        Returns of list transport URLs describing transport used by
        the participant.

        Returns:
            set:
                Set of transport URLs."""
        return set()

    transportURLs = property(getTransportURLs)

    def activate(self):
        pass

    def deactivate(self):
        """
        Deactivates a participant by tearing down all connection
        logic. This needs to be called in case you want to ensure
        that programs can terminate correctly.
        """
        participantDestructionHook.run(self)

    def __enter__(self):
        return self

    def __exit__(self, execType, execValue, traceback):
        self.deactivate()

    @classmethod
    def getConnectors(cls, direction, config):
        if direction not in ('in', 'out'):
            raise ValueError('Invalid direction: %s (valid directions '
                             'are "in" and "out")' % direction)
        if len(config.getTransports()) == 0:
            raise ValueError('No transports specified (config is %s)' % config)

        transports = []
        for transport in config.getTransports():
            factory = rsb.transport.getTransportFactory(transport.getName())
            converters = convertersFromTransportConfig(transport)
            if direction == 'in':
                transports.append(
                    factory.createInPushConnector(converters,
                                                  transport.getOptions()))
            elif direction == 'out':
                transports.append(
                    factory.createOutConnector(converters,
                                               transport.getOptions()))
            else:
                assert False
        return transports


class Informer(Participant):
    """
    Event-sending part of the communication pattern.

    .. codeauthor:: jwienke
    .. codeauthor:: jmoringe
    """

    def __init__(self, scope, config, dataType,
                 configurator=None):
        """
        Constructs a new :obj:`Informer` that publishes :obj:`Events <Event>`
        carrying payloads of type ``type`` on ``scope``.

        Args:
            scope (Scope or accepted by Scope constructor):
                scope of the informer
            config (ParticipantConfig):
                The configuration that should be used by this :obj:`Informer`.
            dataType (types.TypeType):
                A Python object designating the type of objects that will be
                sent via the new :obj:`Informer`. Instances of subtypes are
                permitted as well.
            configurator:
                Out route configurator to manage sending of events through out
                connectors.

        .. todo::

           maybe provide an automatic type identifier deduction for default
           types?

        See Also:
            :obj:`createInformer`
        """
        super(Informer, self).__init__(scope, config)

        self.__logger = getLoggerByClass(self.__class__)

        # TODO check that type can be converted
        if dataType is None:
            raise ValueError("dataType must not be None")
        self.__type = dataType
        self.__sequenceNumber = 0
        self.__configurator = None

        self.__active = False
        self.__mutex = threading.Lock()

        if configurator:
            self.__configurator = configurator
        else:
            connectors = self.getConnectors('out', config)
            for connector in connectors:
                connector.setQualityOfServiceSpec(
                    config.getQualityOfServiceSpec())
            self.__configurator = rsb.eventprocessing.OutRouteConfigurator(
                connectors=connectors)
        self.__configurator.setQualityOfServiceSpec(
            config.getQualityOfServiceSpec())
        self.__configurator.scope = self.scope

        self.__activate()

    def __del__(self):
        self.__logger.debug("Destructing Informer")
        if self.__active:
            self.deactivate()

    def getTransportURLs(self):
        return self.__configurator.getTransportURLs()

    transportURLs = property(getTransportURLs)

    def getType(self):
        """
        Returns the type of data sent by this informer.

        Returns:
            type of sent data
        """
        return self.__type

    type = property(getType)

    def publishData(self, data, userInfos=None, userTimes=None):
        # TODO check activation
        self.__logger.debug("Publishing data '%s'", data)
        event = Event(scope=self.scope,
                      data=data, type=type(data),
                      userInfos=userInfos, userTimes=userTimes)
        return self.publishEvent(event)

    def publishEvent(self, event):
        """
        Publishes a predefined event. The caller must ensure that the
        event has the appropriate scope and type according to the
        :obj:`Informer`'s settings.

        Args:
            event (Event):
                the event to send
        """
        # TODO check activation

        if not event.scope == self.scope \
                and not event.scope.isSubScopeOf(self.scope):
            raise ValueError("Scope %s of event %s is not a sub-scope of "
                             "this informer's scope %s."
                             % (event.scope, event, self.scope))
        if not isinstance(event.data, self.type):
            raise ValueError("The payload %s of event %s does not match "
                             "this informer's type %s."
                             % (event.data, event, self.type))

        with self.__mutex:
            event.id = EventId(self.id, self.__sequenceNumber)
            self.__sequenceNumber += 1
        self.__logger.debug("Publishing event '%s'", event)
        self.__configurator.handle(event)
        return event

    def __activate(self):
        with self.__mutex:
            if self.__active:
                raise RuntimeError("Activate called even though informer "
                                   "was already active")

            self.__logger.info("Activating informer")

            self.__configurator.activate()

            self.__active = True

        self.activate()

    def deactivate(self):
        with self.__mutex:
            if not self.__active:
                self.__logger.info("Deactivate called even though informer "
                                   "was not active")

            self.__logger.info("Deactivating informer")

            self.__active = False

            self.__configurator.deactivate()

        super(Informer, self).deactivate()


class Listener(Participant):
    """
    Event-receiving part of the communication pattern

    .. codeauthor:: jwienke
    .. codeauthor:: jmoringe
    """

    def __init__(self, scope, config,
                 configurator=None,
                 receivingStrategy=None):
        """
        Create a new :obj:`Listener` for ``scope``.

        Args:
            scope (Scope or accepted by Scope constructor):
                The scope of the channel in which the new listener should
                participate.
            config (ParticipantConfig):
                The configuration that should be used by this :obj:`Listener`.
            configurator:
                An in route configurator to manage the receiving of events from
                in connectors and their filtering and dispatching.

        See Also:
            :obj:`createListener`
        """
        super(Listener, self).__init__(scope, config)

        self.__logger = getLoggerByClass(self.__class__)

        self.__filters = []
        self.__handlers = []
        self.__configurator = None
        self.__active = False
        self.__mutex = threading.Lock()

        if configurator:
            self.__configurator = configurator
        else:
            connectors = self.getConnectors('in', config)
            for connector in connectors:
                connector.setQualityOfServiceSpec(
                    config.getQualityOfServiceSpec())
            self.__configurator = rsb.eventprocessing.InRouteConfigurator(
                connectors=connectors,
                receivingStrategy=receivingStrategy)
        self.__configurator.setScope(self.scope)

        self.__activate()

    def __del__(self):
        if self.__active:
            self.deactivate()

    def getTransportURLs(self):
        return self.__configurator.getTransportURLs()

    transportURLs = property(getTransportURLs)

    def __activate(self):
        # TODO commonality with Informer... refactor
        with self.__mutex:
            if self.__active:
                raise RuntimeError("Activate called even though listener "
                                   "was already active")

            self.__logger.info("Activating listener")

            self.__configurator.activate()

            self.__active = True

        self.activate()

    def deactivate(self):
        with self.__mutex:
            if not self.__active:
                raise RuntimeError("Deactivate called even though listener "
                                   "was not active")

            self.__logger.info("Deactivating listener")

            self.__configurator.deactivate()

            self.__active = False

        super(Listener, self).deactivate()

    def addFilter(self, theFilter):
        """
        Appends a filter to restrict the events received by this listener.

        Args:
            theFilter:
                filter to add
        """

        with self.__mutex:
            self.__filters.append(theFilter)
            self.__configurator.filterAdded(theFilter)

    def getFilters(self):
        """
        Returns all registered filters of this listener.

        Returns:
            list of filters
        """

        with self.__mutex:
            return list(self.__filters)

    def addHandler(self, handler, wait=True):
        """
        Adds ``handler`` to the list of handlers this listener invokes
        for received events.

        Args:
            handler:
                Handler to add. callable with one argument, the event.
            wait:
                If set to ``True``, this method will return only after the
                handler has completely been installed and will receive the next
                available message. Otherwise it may return earlier.
        """

        with self.__mutex:
            if handler not in self.__handlers:
                self.__handlers.append(handler)
                self.__configurator.handlerAdded(handler, wait)

    def removeHandler(self, handler, wait=True):
        """
        Removes ``handler`` from the list of handlers this listener
        invokes for received events.

        Args:
            handler:
                Handler to remove.
            wait:
                If set to ``True``, this method will return only after the
                handler has been completely removed from the event processing
                and will not be called anymore from this listener.
        """

        with self.__mutex:
            if handler in self.__handlers:
                self.__configurator.handlerRemoved(handler, wait)
                self.__handlers.remove(handler)

    def getHandlers(self):
        """
        Returns the list of all registered handlers.

        Returns:
            list of callables accepting an Event:
                list of handlers to execute on matches
        """
        with self.__mutex:
            return list(self.__handlers)

__defaultConfigurationOptions = _configDefaultSourcesToDict()
__defaultParticipantConfig = ParticipantConfig.fromDict(
    __defaultConfigurationOptions)


def getDefaultParticipantConfig():
    """
    Returns the current default configuration for new objects.
    """
    return __defaultParticipantConfig


def setDefaultParticipantConfig(config):
    """
    Replaces the default configuration for new objects.

    Args:
        config (ParticipantConfig):
            A ParticipantConfig object which contains the new defaults.
    """
    global __defaultParticipantConfig
    _logger.debug('Setting default participant config to %s', config)
    __defaultParticipantConfig = config

_introspectionDisplayName = __defaultConfigurationOptions.get(
    'introspection.displayname')
_introspectionInitialized = False
_introspectionMutex = threading.RLock()


def _initializeIntrospection():
    global _introspectionInitialized
    import rsb.introspection as introspection
    with _introspectionMutex:
        if not _introspectionInitialized:
            introspection.initialize(_introspectionDisplayName)
            _introspectionInitialized = True


def createParticipant(cls, scope, config, parent=None, **kwargs):
    if config is None:
        config = getDefaultParticipantConfig()
    __registerDefaultTransports()

    if config.introspection:
        _initializeIntrospection()

    participant = cls(scope, config=config, **kwargs)
    participantCreationHook.run(participant, parent=parent)
    return participant


def createListener(scope, config=None, parent=None, **kwargs):
    """
    Creates and returns a new :obj:`Listener` for ``scope``.

    Args:
        scope (Scope or accepted by :obj:`Scope` constructor):
            the scope of the new :obj:`Listener`. Can be a :obj:`Scope` object
            or a string.
        config (ParticipantConfig):
            The configuration that should be used by this :obj:`Listener`.
        parent (Participant or NoneType):
            ``None`` or the :obj:`Participant` which should be considered the
            parent of the new :obj:`Listener`.

    Returns:
        Listener:
            a new :obj:`Listener` object.
    """
    return createParticipant(Listener, scope, config, parent,
                             **kwargs)


def createInformer(scope, config=None, parent=None, dataType=object,
                   **kwargs):
    """
    Creates and returns a new :obj:`Informer` for ``scope``.

    Args:
        scope (Scope or accepted by :obj:`Scope` constructor):
            The scope of the new :obj:`Informer`. Can be a :obj:`Scope` object
            or a string.
        config (ParticipantConfig):
            The configuration that should be used by this :obj:`Informer`.
        parent (Participant or NoneType):
            ``None`` or the :obj:`Participant` which should be considered the
            parent of the new :obj:`Informer`.
        dataType (types.TypeType):
            A Python object designating the type of objects that will be sent
            via the new :obj:`Informer`. Instances of subtypes are permitted as
            well.

    Returns:
        Informer:
            a new :obj:`Informer` object.
    """
    return createParticipant(Informer, scope, config, parent,
                             dataType=dataType,
                             **kwargs)


def createLocalServer(scope, config=None, parent=None,
                      object=None, expose=None, methods=None,
                      **kwargs):
    """
    Create and return a new :obj:`LocalServer` object that exposes its
    methods under ``scope``.

    The keyword parameters object, expose and methods can be used to
    associate an initial set of methods with the newly created server
    object.

    Args:
        scope (Scope or accepted by :obj:`Scope` constructor):
            The scope under which the newly created server should expose its
            methods.
        config (ParticipantConfig):
            The configuration that should be used by this server.
        parent (Participant or NoneType):
            ``None`` or the :obj:`Participant` which should be considered the
            parent of the new server.
        object:
            An object the methods of which should be exposed via the newly
            created server. Has to be supplied in combination with the expose
            keyword parameter.
        expose:
            A list of names of attributes of object that should be expose as
            methods of the newly created server. Has to be supplied in
            combination with the object keyword parameter.
        methods:
            A list or tuple of lists or tuples of the length four:

            * a method name,
            * a callable implementing the method,
            * a type designating the request type of the method and
            * a type designating the reply type of the method.

    Returns:
        rsb.patterns.LocalServer:
            A newly created :obj:`LocalServer` object.
    """
    # Check arguments
    if object is not None and expose is not None and methods is not None:
        raise ValueError('Supply either object and expose or methods')
    if object is None and expose is not None \
            or object is not None and expose is None:
        raise ValueError('object and expose have to supplied together')

    # Create the server object and potentially add methods.
    import rsb.patterns as patterns
    server = createParticipant(patterns.LocalServer,
                               scope, config, parent,
                               **kwargs)
    if object and expose:
        methods = [(name, getattr(object, name), requestType, replyType)
                   for (name, requestType, replyType) in expose]
    if methods:
        for (name, func, requestType, replyType) in methods:
            server.addMethod(name, func, requestType, replyType)
    return server


def createRemoteServer(scope, config=None, parent=None, **kwargs):
    """
    Create a new :obj:`RemoteServer` object for a remote server that
    provides its methods under ``scope``.

    Args:
        scope (Scope or accepted by Scope constructor):
            The scope under which the remote server provides its methods.
        config (ParticipantConfig):
            The transport configuration that should be used for communication
            performed by this server.
        parent (Participant or NoneType):
            ``None`` or the :obj:`Participant` which should be considered the
            parent of the new server.

    Returns:
        rsb.patterns.RemoteServer:
            A newly created :obj:`RemoteServer` object.
    """
    import rsb.patterns as patterns
    return createParticipant(patterns.RemoteServer, scope, config,
                             parent=parent, **kwargs)


def createServer(scope, config=None, parent=None,
                 object=None, expose=None, methods=None,
                 **kwargs):

    """
    Like :obj:`createLocalServer`.

    .. deprecated:: 0.12

       Use :obj:`createLocalServer` instead.
    """
    return createLocalServer(scope, config, parent,
                             object=object, expose=expose, methods=methods,
                             **kwargs)
