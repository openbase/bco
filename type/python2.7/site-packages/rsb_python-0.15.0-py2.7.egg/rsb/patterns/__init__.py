# ============================================================
#
# Copyright (C) 2011, 2012, 2014 Jan Moringen <jmoringe@techfak.uni-bielefeld.de>
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
Package containing pattern implementations like RPC based on the basic
participants :obj:`rsb.Listener` and :obj:`rsb.Informer`.

.. codeauthor:: jmoringe
.. codeauthor:: jwienke
"""

import uuid
import threading

import rsb
import rsb.filter

from rsb.patterns.future import Future, DataFuture
from rsb.eventprocessing import FullyParallelEventReceivingStrategy


# TODO superclass for RSB Errors?
class RemoteCallError(RuntimeError):
    """
    Errors of this class are raised when a call to a remote method
    fails for some reason.

    .. codeauthor:: jmoringe
    """
    def __init__(self, scope, method, message=None):
        super(RemoteCallError, self).__init__(message)
        self._scope = scope
        self._method = method

    def getScope(self):
        return self._scope

    scope = property(getScope)

    def getMethod(self):
        return self._method

    method = property(getMethod)

    def __str__(self):
        s = 'failed to call method "%s" on remote server with scope %s' \
            % (self.method.name, self.scope)
        # TODO(jmoringe): .message seems to be deprecated
        if self.message:
            s += ': %s' % self.message
        return s

######################################################################
#
# Method and Server base classes
#
######################################################################


class Method(rsb.Participant):
    """
    Objects of this class are methods which are associated to a local
    or remote server. Within a server, each method has a unique name.

    This class is primarily intended as a superclass for local and
    remote method classes.

    .. codeauthor:: jmoringe
    """
    # TODO scope and name are redundant
    def __init__(self, scope, config,
                 server, name, requestType, replyType):
        """
        Create a new :obj:`Method` object for the method named ``name``
        provided by ``server``.

        Args:
            server:
                The remote or local server to which the method is associated.
            name (str):
                The name of the method. Unique within a server.
            requestType (types.TypeType):
                The type of the request argument accepted by the method.
            replyType (types.TypeType):
                The type of the replies produced by the method.
        """
        super(Method, self).__init__(scope, config)

        self._server = server
        self._name = name
        self._listener = None
        self._informer = None
        self._requestType = requestType
        self._replyType = replyType

    def getServer(self):
        return self._server

    server = property(getServer)

    def getName(self):
        return self._name

    name = property(getName)

    def getListener(self):
        if self._listener is None:
            self._listener = self.makeListener()
        return self._listener

    listener = property(getListener)

    def getInformer(self):
        if self._informer is None:
            self._informer = self.makeInformer()
        return self._informer

    informer = property(getInformer)

    def getRequestType(self):
        return self._requestType

    requestType = property(getRequestType)

    def getReplyType(self):
        return self._replyType

    replyType = property(getReplyType)

    def deactivate(self):
        if self._informer is not None:
            self._informer.deactivate()
            self._informer = None
        if self._listener is not None:
            self._listener.deactivate()
            self._listener = None

        super(Method, self).deactivate()

    def __str__(self):
        return '<%s "%s" at 0x%x>' % (type(self).__name__, self.name, id(self))

    def __repr__(self):
        return str(self)


class Server(rsb.Participant):
    """
    Objects of this class represent local or remote serves. A server
    is basically a collection of named methods that are bound to a
    specific scope.

    This class is primarily intended as a superclass for local and
    remote server classes.

    .. codeauthor:: jmoringe
    """

    def __init__(self, scope, config):
        """
        Create a new :obj:`Server` object that provides its methods under the
        :obj:`rsb.Scope` ``scope``.

        Args:
            scope (rsb.Scope):
                The under which methods of the server are provided.
            config (rsb.ParticipantConfig):
                The transport configuration that should be used for
                communication performed by this server.
        """
        super(Server, self).__init__(scope, config)

        self.__active = False
        self._methods = {}

        self.activate()

    def __del__(self):
        if self.__active:
            self.deactivate()

    def getMethods(self):
        return self._methods.values()

    methods = property(getMethods)

    def getMethod(self, name):
        if name in self._methods:
            return self._methods[name]

    def addMethod(self, method):
        self._methods[method.name] = method

    def removeMethod(self, method):
        del self._methods[method.name]

    # State management

    def activate(self):
        self.__active = True

        super(Server, self).activate()

    def deactivate(self):
        if not self.__active:
            raise RuntimeError('Trying to deactivate inactive server')

        self.__active = False

        for m in self._methods.values():
            m.deactivate()

        super(Server, self).deactivate()

    # Printing

    def __str__(self):
        return '<%s with %d method(s) at 0x%x>' % (type(self).__name__,
                                                   len(self._methods),
                                                   id(self))

    def __repr__(self):
        return str(self)

######################################################################
#
# Local Server
#
######################################################################


class LocalMethod(Method):
    """
    Objects of this class implement and make available methods of a
    local server.

    The actual behavior of methods is implemented by invoking
    arbitrary user-supplied callables.

    .. codeauthor:: jmoringe
    """
    def __init__(self, scope, config,
                 server, name, func, requestType, replyType,
                 allowParallelExecution):
        super(LocalMethod, self).__init__(
            scope, config, server, name, requestType, replyType)

        self._allowParallelExecution = allowParallelExecution
        self._func = func
        self.listener  # force listener creation

    def makeListener(self):
        receivingStrategy = None
        if self._allowParallelExecution:
            receivingStrategy = FullyParallelEventReceivingStrategy()
        listener = rsb.createListener(self.scope, self.config,
                                      parent=self,
                                      receivingStrategy=receivingStrategy)
        listener.addFilter(rsb.filter.MethodFilter(method='REQUEST'))
        listener.addHandler(self._handleRequest)
        return listener

    def makeInformer(self):
        return rsb.createInformer(self.scope, self.config,
                                  parent=self,
                                  dataType=object)

    def _handleRequest(self, request):
        # Call the callable implementing the behavior of this
        # method. If it does not take an argument
        # (i.e. self.requestType is type(None)), call it without
        # argument. Otherwise pass the payload of the request event to
        # it.
        userInfos = {}
        causes = [request.id]
        isError = False
        try:
            if self.requestType is type(None):
                assert(request.data is None)
                result = self._func()
            elif self.requestType is rsb.Event:
                result = self._func(request)
            else:
                result = self._func(request.data)
            resultType = type(result)
        except Exception, e:
            isError = True
            userInfos['rsb:error?'] = '1'
            result = str(e)
            resultType = str

        # If the returned result is an event, use it as reply event
        # (after adding the request as cause). Otherwise add all
        # necessary meta-data.
        if isinstance(result, rsb.Event):
            reply = result
            reply.method = 'REPLY'
            reply.causes += causes
        else:
            # This check is required because the reply informer is
            # created with type 'object' to enable throwing exceptions
            if not isError and not isinstance(result, self.replyType):
                raise ValueError("The result '%s' (of type %s) "
                                 "of method %s does not match "
                                 "the method's declared return type %s."
                                 % (result, resultType,
                                    self.name, self.replyType))
            reply = rsb.Event(scope=self.informer.scope,
                              method='REPLY',
                              data=result,
                              type=resultType,
                              userInfos=userInfos,
                              causes=causes)

        # Publish the reply event.
        self.informer.publishEvent(reply)


class LocalServer(Server):
    """
    Objects of this class associate a collection of method objects
    which are implemented by callback functions with a scope under
    which these methods are exposed for remote clients.

    .. codeauthor:: jmoringe
    """
    def __init__(self, scope, config):
        """
        Creates a new :obj:`LocalServer` object that exposes methods under
        the :obj:`rsb.Scope` ``scope``.

        Args:
            scope (rsb.Scope):
                The scope under which the methods of the newly created server
                should be provided.
            config (rsb.ParticipantConfig):
                The transport configuration that should be used for
                communication performed by this server.

        See Also:
            :obj:`rsb.createServer`
        """
        super(LocalServer, self).__init__(scope, config)

    def addMethod(self, name, func, requestType=object, replyType=object,
                  allowParallelExecution=False):
        """
        Add a method named ``name`` that is implemented by ``func``.

        Args:
            name (str):
                The name of of the new method.
            func:
                A callable object or a single argument that implements the
                desired behavior of the new method.
            requestType (types.TypeType):
                A type object indicating the type of request data passed to the
                method.
            replyType:
                A type object indicating the type of reply data of the method.
            allowParallelExecution(bool):
                if set to True, the method will be called fully asynchronously
                and even multiple calls may enter the method in parallel. Also,
                no ordering is guaranteed anymore.

        Returns:
            LocalMethod:
                The newly created method.
        """
        scope = self.scope.concat(rsb.Scope('/' + name))
        method = rsb.createParticipant(
            LocalMethod, scope, self.config,
            parent=self,
            server=self,
            name=name,
            func=func,
            requestType=requestType,
            replyType=replyType,
            allowParallelExecution=allowParallelExecution)
        super(LocalServer, self).addMethod(method)
        return method

    def removeMethod(self, method):
        if isinstance(method, str):
            method = self.getMethod(method)
        super(LocalServer, self).removeMethod(method)

######################################################################
#
# Remote Server
#
######################################################################


class RemoteMethod(Method):
    """
    Objects of this class represent methods provided by a remote
    server. Method objects are callable like regular bound method
    objects.

    .. codeauthor:: jmoringe
    """
    def __init__(self, scope, config, server, name, requestType, replyType):
        super(RemoteMethod, self).__init__(scope, config,
                                           server, name,
                                           requestType, replyType)

        self._calls = {}
        self._lock = threading.RLock()

    def makeListener(self):
        listener = rsb.createListener(self.scope, self.config,
                                      parent=self)
        listener.addFilter(rsb.filter.MethodFilter(method='REPLY'))
        listener.addHandler(self._handleReply)
        return listener

    def makeInformer(self):
        return rsb.createInformer(self.scope, self.config,
                                  parent=self,
                                  dataType=self.requestType)

    def _handleReply(self, event):
        if not event.causes:
            return

        key = event.causes[0]
        with self._lock:
            # We can receive reply events which aren't actually
            # intended for us. We ignore these.
            if key not in self._calls:
                return

            # The result future
            result = self._calls[key]
            del self._calls[key]
        if 'rsb:error?' in event.metaData.userInfos:
            result.setError(event.data)
        else:
            result.set(event)

    def __call__(self, arg=None):
        """
        Call the method synchronously with argument ``arg``, returning
        the value returned by the remote method.

        If ``arg`` is an instance of :obj:`Event`, an :obj:`Event` containing
        the object returned by the remote method as payload is
        returned. If ``arg`` is of any other type, return the object
        that was returned by the remote method.

        The call to this method blocks until a result is available or
        an error occurs.

        Examples:
            >>> myServer.echo('bla')
            'bla'
            >>> myServer.echo(Event(scope=myServer.scope, data='bla',
            >>>                     type=str))
            Event[id = ..., data = 'bla', ...]

        Args:
            arg:
                The argument object that should be passed to the remote method.
                A converter has to be available for the type of ``arg``.

        Returns:
            The object that was returned by the remote method.

        Raises:
            RemoteCallError:
                If invoking the remote method fails or the remote method itself
                produces an error.

        See Also:
            :obj:`async`
        """
        return self.async(arg).get()

    def async(self, arg=None):
        """
        Call the method asynchronously with argument ``arg``, returning
        a :obj:`Future` instance that can be used to retrieve the result.

        If ``arg`` is an instance of :obj:`Event`, the result of the method
        call is an :obj:`Event` containing the object returned by the
        remote method as payload. If ``arg`` is of any other type, the
        result is the payload of the method call is the object that
        was returned by the remote method.

        The call to this method returns immediately, even if the
        remote method did produce a result yet. The returned :obj:`Future`
        instance has to be used to retrieve the result.

        Args:
            arg:
                The argument object that should be passed to the remote method.
                A converter has to be available for the type of ``arg``.

        Returns:
            Future or DataFuture:
                A :obj:`Future` or :obj:`DataFuture` instance that can be used
                to check the success of the method call, wait for the result
                and retrieve the result.

        Raises:
            RemoteCallError:
                If an error occurs before the remote was invoked.

        See Also:
            :obj:`__call__`

        Examples:
            >>> myServer.echo.async('bla')
            <Future running at 3054cd0>
            >>> myServer.echo.async('bla').get()
            'bla'
            >>> myServer.echo.async(Event(scope=myServer.scope,
            ...                           data='bla', type=str)).get()
            Event[id = ..., data = 'bla', ...]
        """
        self.listener  # Force listener creation

        # When the caller supplied an event, adjust the meta-data and
        # create a future that will return an event.
        if isinstance(arg, rsb.Event):
            event = arg
            event.scope = self.informer.scope
            event.method = 'REQUEST'
            result = Future()
        # Otherwise, create a new event with suitable meta-data and a
        # future that will return the payload of the reply event.
        else:
            event = rsb.Event(scope=self.informer.scope,
                              method='REQUEST',
                              data=arg,
                              type=type(arg))
            result = DataFuture()

        # Publish the constructed request event and record the call as
        # in-progress, waiting for a reply.
        try:
            with self._lock:
                event = self.informer.publishEvent(event)
                self._calls[event.id] = result
        except Exception, e:
            raise RemoteCallError(self.server.scope, self, message=repr(e))
        return result

    def __str__(self):
        return '<%s "%s" with %d in-progress calls at 0x%x>' \
            % (type(self).__name__, self.name, len(self._calls), id(self))

    def __repr__(self):
        return str(self)


class RemoteServer(Server):
    """
    Objects of this class represent remote servers in a way that
    allows calling methods on them as if they were local.

    .. codeauthor:: jmoringe
    """
    def __init__(self, scope, config):
        """
        Create a new :obj:`RemoteServer` object that provides its methods
        under the scope ``scope``.

        Args:
            scope (rsb.Scope):
                The common super-scope under which the methods of the remote
                created server are provided.
            config (rsb.ParticipantConfig):
                The configuration that should be used by this server.

        See Also:
            :obj:`rsb.createRemoteServer`
        """
        super(RemoteServer, self).__init__(scope, config)

    def ensureMethod(self, name):
        method = super(RemoteServer, self).getMethod(name)
        if method is None:
            scope = self.scope.concat(rsb.Scope('/' + name))
            method = rsb.createParticipant(RemoteMethod, scope, self.config,
                                           parent=self,
                                           server=self,
                                           name=name,
                                           requestType=object,
                                           replyType=object)
            self.addMethod(method)
        return method

    def getMethod(self, name):
        return self.ensureMethod(name)

    def __getattr__(self, name):
        # Treat missing attributes as methods.
        try:
            return super(RemoteServer, self).__getattr__(name)
        except AttributeError:
            return self.ensureMethod(name)
