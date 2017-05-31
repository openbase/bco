# ============================================================
#
# Copyright (C) 2011, 2012 Jan Moringen <jmoringe@techfak.uni-bielefeld.de>
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
A module containing serialization methods used by several transports which
convert :obj:`rsb.Event` instances to protocol buffers based data types and
vice versa.

.. codeauthor:: jmoringe
.. codeauthor:: jwienke
"""

import itertools
import uuid

import rsb
from rsb.util import unixMicrosecondsToTime, timeToUnixMicroseconds

from rsb.protocol.EventId_pb2 import EventId
from rsb.protocol.EventMetaData_pb2 import UserInfo, UserTime
from rsb.protocol.Notification_pb2 import Notification
from rsb.protocol.FragmentedNotification_pb2 import FragmentedNotification


def notificationToEvent(notification, wireData, wireSchema, converter):
    """
    Build event from notification.
    """
    event = rsb.Event(
        rsb.EventId(uuid.UUID(bytes=notification.event_id.sender_id),
                    notification.event_id.sequence_number))
    event.scope = rsb.Scope(notification.scope)
    if notification.HasField("method"):
        event.method = notification.method
    event.type = converter.getDataType()
    event.data = converter.deserialize(wireData, wireSchema)

    # Meta data
    event.metaData.createTime = unixMicrosecondsToTime(
        notification.meta_data.create_time)
    event.metaData.sendTime = unixMicrosecondsToTime(
        notification.meta_data.send_time)
    event.metaData.setReceiveTime()
    for info in notification.meta_data.user_infos:
        event.metaData.setUserInfo(info.key, info.value)
    for time in notification.meta_data.user_times:
        event.metaData.setUserTime(time.key,
                                   unixMicrosecondsToTime(time.timestamp))

    # Causes
    for cause in notification.causes:
        id = rsb.EventId(uuid.UUID(bytes=cause.sender_id),
                         cause.sequence_number)
        event.addCause(id)

    return event


def eventToNotification(notification, event, wireSchema, data, metaData=True):
    # Identification information
    notification.event_id.sender_id = event.senderId.bytes
    notification.event_id.sequence_number = event.sequenceNumber

    # Payload [fragment]
    notification.data = str(data)

    # Fill meta-data
    if metaData:
        notification.scope = event.scope.toString()
        if event.method is not None:
            notification.method = event.method
        notification.wire_schema = wireSchema

        md = notification.meta_data
        md.create_time = timeToUnixMicroseconds(event.metaData.createTime)
        md.send_time = timeToUnixMicroseconds(event.metaData.sendTime)
        for (k, v) in event.metaData.userInfos.items():
            info = md.user_infos.add()
            info.key = k
            info.value = v
        for (k, v) in event.metaData.userTimes.items():
            time = md.user_times.add()
            time.key = k
            time.timestamp = timeToUnixMicroseconds(v)
        # Add causes
        for cause in event.causes:
            id = notification.causes.add()
            id.sender_id = cause.participantId.bytes
            id.sequence_number = cause.sequenceNumber


def eventToNotifications(event, converter, maxFragmentSize):
    wireData, wireSchema = converter.serialize(event.data)

    remaining, offset, fragments = len(wireData), 0, []
    for i in itertools.count():
        # Create fragment container
        fragment = FragmentedNotification()
        # Overwritten below if necessary
        fragment.num_data_parts = 1
        fragment.data_part = i
        fragments.append(fragment)

        # Fill notification object for data fragment
        #
        # We reserve at least 5 bytes for the payload: up to 4 bytes
        # for the field header and one byte for the payload data.
        room = maxFragmentSize - fragment.ByteSize()
        if room < 5:
            raise ValueError('The event %s cannot be encoded in a '
                             'notification because the serialized meta-data '
                             'would not fit into a single fragment' % event)
        # allow for 4 byte field header
        fragmentSize = min(room - 4, remaining)
        eventToNotification(fragment.notification, event,
                            wireSchema=wireSchema,
                            data=wireData[offset:offset + fragmentSize],
                            metaData=(i == 0))
        offset += fragmentSize
        remaining -= fragmentSize

        if remaining == 0:
            break

    # Adjust fragment count in all fragments, if we actually produced
    # more than one.
    if len(fragments) > 1:
        for fragment in fragments:
            fragment.num_data_parts = len(fragments)

    return fragments
