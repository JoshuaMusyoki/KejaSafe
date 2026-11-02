
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



public interface SubmitKycMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      SubmitKycMutation.Data,
      SubmitKycMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val documentType: String,
    val documentNumber: String,
    val documentFrontImageUrl: String,
    val documentBackImageUrl: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val selfieImageUrl: String
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var documentType: String
        public var documentNumber: String
        public var documentFrontImageUrl: String
        public var documentBackImageUrl: String?
        public var selfieImageUrl: String
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          documentType: String,documentNumber: String,documentFrontImageUrl: String,selfieImageUrl: String,
          block_: Builder.() -> Unit
        ): Variables {
          var documentType= documentType
            var documentNumber= documentNumber
            var documentFrontImageUrl= documentFrontImageUrl
            var documentBackImageUrl: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var selfieImageUrl= selfieImageUrl
            

          return object : Builder {
            override var documentType: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { documentType = value_ }
              
            override var documentNumber: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { documentNumber = value_ }
              
            override var documentFrontImageUrl: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { documentFrontImageUrl = value_ }
              
            override var documentBackImageUrl: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { documentBackImageUrl = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var selfieImageUrl: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { selfieImageUrl = value_ }
              
            
          }.apply(block_)
          .let {
            Variables(
              documentType=documentType,documentNumber=documentNumber,documentFrontImageUrl=documentFrontImageUrl,documentBackImageUrl=documentBackImageUrl,selfieImageUrl=selfieImageUrl,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val kycSubmission_insert: KycSubmissionKey,
    val user_update: UserKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "SubmitKyc"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun SubmitKycMutation.ref(
  
    documentType: String,documentNumber: String,documentFrontImageUrl: String,selfieImageUrl: String,

  
    block_: SubmitKycMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    SubmitKycMutation.Data,
    SubmitKycMutation.Variables
  > =
  ref(
    
      SubmitKycMutation.Variables.build(
        documentType=documentType,documentNumber=documentNumber,documentFrontImageUrl=documentFrontImageUrl,selfieImageUrl=selfieImageUrl,
  
    block_
      )
    
  )

public suspend fun SubmitKycMutation.execute(

  
    
      documentType: String,documentNumber: String,documentFrontImageUrl: String,selfieImageUrl: String,

  
    block_: SubmitKycMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    SubmitKycMutation.Data,
    SubmitKycMutation.Variables
  > =
  ref(
    
      documentType=documentType,documentNumber=documentNumber,documentFrontImageUrl=documentFrontImageUrl,selfieImageUrl=selfieImageUrl,
  
    block_
    
  ).execute()


