
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

package com.rotd.havenpad.dataconnect



public interface SendMessageMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      SendMessageMutation.Data,
      SendMessageMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val conversationId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val content: String,
    val type: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val message_insert: MessageKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "SendMessage"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun SendMessageMutation.ref(
  
    conversationId: java.util.UUID,content: String,type: String,

  
  
): com.google.firebase.dataconnect.MutationRef<
    SendMessageMutation.Data,
    SendMessageMutation.Variables
  > =
  ref(
    
      SendMessageMutation.Variables(
        conversationId=conversationId,content=content,type=type,
  
      )
    
  )

public suspend fun SendMessageMutation.execute(

  
    
      conversationId: java.util.UUID,content: String,type: String,

  

  ): com.google.firebase.dataconnect.MutationResult<
    SendMessageMutation.Data,
    SendMessageMutation.Variables
  > =
  ref(
    
      conversationId=conversationId,content=content,type=type,
  
    
  ).execute()


