
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



public interface UpdateMyProfileMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      UpdateMyProfileMutation.Data,
      UpdateMyProfileMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val fullName: String,
    val username: String,
    val major: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val yearOfStudy: com.google.firebase.dataconnect.OptionalVariable<Int?>,
    val roommatePreferences: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val phone: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val businessName: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val profilePictureUrl: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var fullName: String
        public var username: String
        public var major: String?
        public var yearOfStudy: Int?
        public var roommatePreferences: String?
        public var phone: String?
        public var businessName: String?
        public var profilePictureUrl: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          fullName: String,username: String,
          block_: Builder.() -> Unit
        ): Variables {
          var fullName= fullName
            var username= username
            var major: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var yearOfStudy: com.google.firebase.dataconnect.OptionalVariable<Int?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var roommatePreferences: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var phone: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var businessName: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var profilePictureUrl: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var fullName: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { fullName = value_ }
              
            override var username: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { username = value_ }
              
            override var major: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { major = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var yearOfStudy: Int?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { yearOfStudy = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var roommatePreferences: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { roommatePreferences = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var phone: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { phone = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var businessName: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { businessName = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var profilePictureUrl: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { profilePictureUrl = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              fullName=fullName,username=username,major=major,yearOfStudy=yearOfStudy,roommatePreferences=roommatePreferences,phone=phone,businessName=businessName,profilePictureUrl=profilePictureUrl,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user_update: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateMyProfile"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateMyProfileMutation.ref(
  
    fullName: String,username: String,

  
    block_: UpdateMyProfileMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateMyProfileMutation.Data,
    UpdateMyProfileMutation.Variables
  > =
  ref(
    
      UpdateMyProfileMutation.Variables.build(
        fullName=fullName,username=username,
  
    block_
      )
    
  )

public suspend fun UpdateMyProfileMutation.execute(

  
    
      fullName: String,username: String,

  
    block_: UpdateMyProfileMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateMyProfileMutation.Data,
    UpdateMyProfileMutation.Variables
  > =
  ref(
    
      fullName=fullName,username=username,
  
    block_
    
  ).execute()


