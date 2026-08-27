
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



public interface JoinConversationMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      JoinConversationMutation.Data,
      JoinConversationMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val conversationId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val conversationParticipant_insert: ConversationParticipantKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "JoinConversation"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun JoinConversationMutation.ref(
  
    conversationId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    JoinConversationMutation.Data,
    JoinConversationMutation.Variables
  > =
  ref(
    
      JoinConversationMutation.Variables(
        conversationId=conversationId,
  
      )
    
  )

public suspend fun JoinConversationMutation.execute(

  
    
      conversationId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    JoinConversationMutation.Data,
    JoinConversationMutation.Variables
  > =
  ref(
    
      conversationId=conversationId,
  
    
  ).execute()


