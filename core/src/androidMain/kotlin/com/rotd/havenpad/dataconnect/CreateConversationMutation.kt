
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



public interface CreateConversationMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      CreateConversationMutation.Data,
      CreateConversationMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val name: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var name: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          
          block_: Builder.() -> Unit
        ): Variables {
          var name: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var name: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { name = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              name=name,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val conversation_insert: ConversationKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateConversation"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateConversationMutation.ref(
  
    

  
    block_: CreateConversationMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    CreateConversationMutation.Data,
    CreateConversationMutation.Variables
  > =
  ref(
    
      CreateConversationMutation.Variables.build(
        
  
    block_
      )
    
  )

public suspend fun CreateConversationMutation.execute(

  
    
      

  
    block_: CreateConversationMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    CreateConversationMutation.Data,
    CreateConversationMutation.Variables
  > =
  ref(
    
      
  
    block_
    
  ).execute()


