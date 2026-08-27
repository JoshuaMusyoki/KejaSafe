
@file:Suppress(
  "KotlinRedundantDiagnosticSuppress",
  "LocalVariableName",
  "MayBeConstant",
  "RedundantVisibilityModifier",
  "RedundantCompanionReference",
  "RemoveEmptyClassBody",
  "SpellCheckingInspection",
  "LocalVariableName",
  "unused",
)

package com.rotd.kejasafe.dataconnect


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface MyConversationsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      MyConversationsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val conversationParticipants: List<ConversationParticipantsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ConversationParticipantsItem(
  
    val conversation: Conversation
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Conversation(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String?,
    val messages_on_conversation: List<MessagesOnConversationItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class MessagesOnConversationItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val content: String,
    val type: String,
    val timestamp: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val sender: Sender
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Sender(
  
    val id: String,
    val username: String,
    val fullName: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "MyConversations"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun MyConversationsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    MyConversationsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun MyConversationsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    MyConversationsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun MyConversationsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<MyConversationsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

