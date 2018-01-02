# ============================================================
#
# Copyright (C) 2011 Jan Moringen <jmoringe@techfak.uni-bielefeld.de>
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
Contains an implementation of the future pattern.

.. codeauthor:: jmoringe
"""

import threading


class FutureError(RuntimeError):
    def __init__(self, *args):
        super(FutureError, self).__init__(*args)


class FutureTimeout(FutureError):
    def __init__(self, *args):
        super(FutureTimeout, self).__init__(*args)


class FutureExecutionError(FutureError):
    def __init__(self, *args):
        super(FutureExecutionError, self).__init__(*args)


class Future(object):
    """
    Objects of this class represent the results of in-progress
    operations.

    Methods of this class allow checking the state of the represented
    operation, waiting for the operation to finish and retrieving the
    result of the operation.

    .. todo::

       Support Python's native future protocol?

    .. codeauthor:: jmoringe

    See Also:
        <http://docs.python.org/dev/library/concurrent.futures.html>_
    """

    def __init__(self):
        """
        Create a new :obj:`Future` object that represents an in-progress
        operation for which a result is not yet available.
        """
        self.__error = False
        self.__result = None

        self.__lock = threading.Lock()
        self.__condition = threading.Condition(lock=self.__lock)

    def isDone(self):
        """
        Check whether the represented operation is still in progress.

        Returns:
            bool:
                ``True`` is the represented operation finished successfully or
                failed.
        """
        with self.__lock:
            return self.__result is not None

    done = property(isDone)

    def get(self, timeout=0):
        """
        Try to obtain and then return the result of the represented
        operation.

        If necessary, wait for the operation to complete, and then
        retrieve its result.

        Args:
            timeout (float, optional):
                The amount of time in seconds in which the operation has to
                complete.

        Returns:
            The result of the operation if it did complete successfully.

        Raises:
            FutureExecutionException:
                If the operation represented by the Future object failed.
            FutureTimeoutException:
                If the result does not become available within the amount of
                time specified via ``timeout``.
        """
        with self.__lock:
            while self.__result is None:
                if timeout <= 0:
                    self.__condition.wait()
                else:
                    # TODO(jmoringe): this is probably wrong since there
                    #                 may be spurious wakeups
                    self.__condition.wait(timeout=timeout)
                    if self.__result is None:
                        raise FutureTimeout(
                            'Timeout while waiting for result; '
                            'Waited %s seconds.' % timeout)

        if self.__error:
            raise FutureExecutionError('Failed to execute operation: %s' %
                                       self.__result)

        return self.__result

    def set(self, result):
        """
        Set the result of the :obj:`Future` to ``result`` and wake all
        threads waiting for the result.

        Args:
            result:
                The result of the :obj:`Future` object.
        """
        with self.__lock:
            self.__result = result
            self.__condition.notifyAll()

    def setError(self, message):
        """
        Mark the operation represented by the :obj:`Future` object as
        failed, set ``message`` as the error message and notify all
        threads waiting for the result.

        Args:
            message (str):
                An error message that explains why/how the operation failed.
        """
        with self.__lock:
            self.__result = message
            self.__error = True
            self.__condition.notify()

    def __str__(self):
        with self.__lock:
            if self.__result is None:
                state = 'running'
            elif self.__error:
                state = 'failed'
            else:
                state = 'completed'
        return '<%s %s at 0x%x>' % (type(self).__name__, state, id(self))

    def __repr__(self):
        return str(self)


class DataFuture(Future):
    """
    Instances of this class are like ordinary :obj:`Future`s, the only
    difference being that the :obj:`get` method returns the payload of an
    :obj:`Event` object.

    .. codeauthor:: jmoringe
    """
    def get(self, timeout=0):
        return super(DataFuture, self).get(timeout=timeout).data
