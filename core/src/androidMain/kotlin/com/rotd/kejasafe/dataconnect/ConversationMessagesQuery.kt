
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


public interface ConversationMessagesQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      ConversationMessagesQuery.Data,
      ConversationMessagesQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val conversationId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val messages: List<MessagesItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class MessagesItem(
  
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
    val fullName: String,
    val profilePictureUrl: String?
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "ConversationMessages"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ConversationMessagesQuery.ref(
  
    conversationId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.QueryRef<
    ConversationMessagesQuery.Data,
    ConversationMessagesQuery.Variables
  > =
  ref(
    
      ConversationMessagesQuery.Variables(
        conversationId=conversationId,
  
      )
    
  )

public suspend fun ConversationMessagesQuery.execute(

  
    
      conversationId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.QueryResult<
    ConversationMessagesQuery.Data,
    ConversationMessagesQuery.Variables
  > =
  ref(
    
      conversationId=conversationId,
  
    
  ).execute()


  public fun ConversationMessagesQuery.flow(
    
      conversationId: java.util.UUID,

  
    
    ): kotlinx.coroutines.flow.Flow<ConversationMessagesQuery.Data> =
    ref(
        
          conversationId=conversationId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

