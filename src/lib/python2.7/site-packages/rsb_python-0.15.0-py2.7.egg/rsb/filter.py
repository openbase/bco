# ============================================================
#
# Copyright (C) 2010 by Johannes Wienke <jwienke at techfak dot uni-bielefeld dot de>
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
Contains filters which can be used to restrict the amount of events received by
clients.

.. codeauthor:: jwienke
.. codeauthor:: jmoringe
"""

import rsb.util
from threading import Condition

FilterAction = rsb.util.Enum("FilterAction", ["ADD", "REMOVE", "UPDATE"])


class AbstractFilter(object):
    """
    Interface for concrete filters.

    .. codeauthor:: jwienke
    """

    def match(self, event):
        """
        Matches this filter against a given event.

        Args:
            event:
                event to match against

        Returns:
            True if this filter matches the event, else False
        """
        pass


class ScopeFilter(AbstractFilter):
    """
    A filter to restrict the scope for events.

    .. codeauthor:: jwienke
    """

    def __init__(self, scope):
        """
        Constructs a new scope filter with a given scope to restrict to.

        Args:
            scope:
                top-level scope to accept and al child scopes
        """
        self.__scope = scope

    def getScope(self):
        """
        Returns the top-level scope this filter matches for.

        Returns:
            scope
        """
        return self.__scope

    def match(self, event):
        return event.scope == self.__scope \
            or event.scope.isSubScopeOf(self.__scope)


class OriginFilter(AbstractFilter):
    """
    Matching events have to originate at a particular participant.

    .. codeauthor:: jmoringe
    """

    def __init__(self, origin, invert=False):
        """
        Args:
            origin:
                The id of the :obj:`Participant` from which matching events
                should originate.
            invert (bool):
                Controls whether matching results should inverted (i.e.
                matching events ``not`` originating form ``origin``).
        """
        self.__origin = origin
        self.__invert = invert

    def getOrigin(self):
        return self.__origin

    origin = property(getOrigin)

    def getInvert(self):
        return self.__invert

    invert = property(getInvert)

    def match(self, event):
        result = self.origin == event.senderId
        if self.invert:
            return not result
        else:
            return result

    def __str__(self):
        inverted = ''
        if self.invert:
            inverted = 'not '
        return '<%s %sfrom %s at 0x%x>' % (type(self).__name__,
                                           inverted,
                                           self.origin,
                                           id(self))

    def __repr__(self):
        return '%s("%s", invert = %s)' \
            % (type(self).__name__, self.origin, self.invert)

class CauseFilter(AbstractFilter):
    """
    Filter events based on their cause vectors.

    .. codeauthor:: jmoringe
    """

    def __init__(self, cause, invert=False):
        """
        Args:
            cause:
                The id of the :obj:`Event` that should be in the cause
                vector of matching events.
            invert (bool):
                Controls whether matching results should inverted
                (i.e.  matching events that do ``not`` have the
                specified event id in their cause vector).
        """
        self.__cause = cause
        self.__invert = invert

    def getCause(self):
        return self.__cause

    cause = property(getCause)

    def getInvert(self):
        return self.__invert

    invert = property(getInvert)

    def match(self, event):
        result = self.cause in event.causes
        if self.invert:
            return not result
        else:
            return result

    def __str__(self):
        inverted = ''
        if self.invert:
            inverted = 'not '
        return '<%s %scaused-by %s at 0x%x>' \
            % (type(self).__name__, inverted, self.cause, id(self))

    def __repr__(self):
        return '%s("%s", invert = %s)' \
            % (type(self).__name__, self.cause, self.invert)


class MethodFilter(AbstractFilter):
    """
    Matching events have (not) have a particular value in their method
    field.

    .. codeauthor:: jmoringe
    """

    def __init__(self, method, invert=False):
        """
        Args:
            method (str):
                The method string that matching events have to have in their
                method field.
            invert (bool):
                Controls whether matching results should inverted (i.e.
                matching events ``not`` having  ``method`` in their method
                field).
        """
        self.__method = method
        self.__invert = invert

    def getMethod(self):
        return self.__method

    method = property(getMethod)

    def getInvert(self):
        return self.__invert

    invert = property(getInvert)

    def match(self, event):
        result = self.method == event.method
        if self.invert:
            return not result
        else:
            return result

    def __str__(self):
        inverted = ''
        if self.invert:
            inverted = 'not '
            return '<%s %sfrom %s at 0x%x>' \
                % (type(self).__name__, inverted, self.method, id(self))

    def __repr__(self):
        return '%s("%s", invert = %s)' \
            % (type(self).__name__, self.method, self.invert)


class RecordingTrueFilter(AbstractFilter):

    def __init__(self):
        self.events = []
        self.condition = Condition()

    def match(self, event):
        with self.condition:
            self.events.append(event)
            self.condition.notifyAll()
            return True


class RecordingFalseFilter(AbstractFilter):

    def __init__(self):
        self.events = []
        self.condition = Condition()

    def match(self, event):
        with self.condition:
            self.events.append(event)
            self.condition.notifyAll()
            return False


class TrueFilter(AbstractFilter):
    def match(self, event):
        return True


class FalseFilter(AbstractFilter):
    def match(self, event):
        return False
