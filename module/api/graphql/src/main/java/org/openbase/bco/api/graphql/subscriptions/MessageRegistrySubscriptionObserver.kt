package org.openbase.bco.api.graphql.subscriptions

import com.google.common.collect.ImmutableList
import org.openbase.bco.api.graphql.error.ServerError
import org.openbase.bco.api.graphql.schema.RegistrySchemaModule
import org.openbase.bco.registry.remote.Registries
import org.openbase.jul.pattern.provider.DataProvider
import org.openbase.type.domotic.communication.UserMessageType
import org.openbase.type.domotic.registry.MessageRegistryDataType

class MessageRegistrySubscriptionObserver() :
    AbstractObserverMapper<DataProvider<MessageRegistryDataType.MessageRegistryData>, MessageRegistryDataType.MessageRegistryData, List<UserMessageType.UserMessage>>() {
    private val userMessages: MutableList<UserMessageType.UserMessage>

    init {
        Registries.getMessageRegistry(
            ServerError.BCO_TIMEOUT_SHORT,
            ServerError.BCO_TIMEOUT_TIME_UNIT
        )
        userMessages = ArrayList(
            RegistrySchemaModule.getUserMessages()
        )
    }

    @Throws(Exception::class)
    override fun update(
        source: DataProvider<MessageRegistryDataType.MessageRegistryData>,
        target: MessageRegistryDataType.MessageRegistryData,
    ) {
        val newUserMessages: ImmutableList<UserMessageType.UserMessage> =
            RegistrySchemaModule.getUserMessages()
        if (newUserMessages == userMessages) {
            // nothing has changed
            return
        }

        // store update
        userMessages.clear()
        userMessages.addAll(newUserMessages)
        super.update(source, target)
    }

    @Throws(Exception::class)
    override fun mapData(
        source: DataProvider<MessageRegistryDataType.MessageRegistryData>,
        data: MessageRegistryDataType.MessageRegistryData,
    ): List<UserMessageType.UserMessage> {
        return userMessages
    }
}
