
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



public interface UpsertMyUserMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      UpsertMyUserMutation.Data,
      UpsertMyUserMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val email: String,
    val username: String,
    val fullName: String,
    val role: String,
    val googleId: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val phone: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var email: String
        public var username: String
        public var fullName: String
        public var role: String
        public var googleId: String?
        public var phone: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          email: String,username: String,fullName: String,role: String,
          block_: Builder.() -> Unit
        ): Variables {
          var email= email
            var username= username
            var fullName= fullName
            var role= role
            var googleId: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var phone: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var email: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { email = value_ }
              
            override var username: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { username = value_ }
              
            override var fullName: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fullName = value_ }
              
            override var role: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { role = value_ }
              
            override var googleId: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { googleId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var phone: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { phone = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              email=email,username=username,fullName=fullName,role=role,googleId=googleId,phone=phone,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_upsert: UserKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpsertMyUser"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpsertMyUserMutation.ref(
  
    email: String,username: String,fullName: String,role: String,

  
    block_: UpsertMyUserMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpsertMyUserMutation.Data,
    UpsertMyUserMutation.Variables
  > =
  ref(
    
      UpsertMyUserMutation.Variables.build(
        email=email,username=username,fullName=fullName,role=role,
  
    block_
      )
    
  )

public suspend fun UpsertMyUserMutation.execute(

  
    
      email: String,username: String,fullName: String,role: String,

  
    block_: UpsertMyUserMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpsertMyUserMutation.Data,
    UpsertMyUserMutation.Variables
  > =
  ref(
    
      email=email,username=username,fullName=fullName,role=role,
  
    block_
    
  ).execute()


