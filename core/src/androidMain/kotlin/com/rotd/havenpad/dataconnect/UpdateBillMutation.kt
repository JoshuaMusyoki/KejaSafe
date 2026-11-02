
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



public interface UpdateBillMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      UpdateBillMutation.Data,
      UpdateBillMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val propertyId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val totalAmount: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val depositIncluded: com.google.firebase.dataconnect.OptionalVariable<Boolean?>,
    val electricityNote: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var id: java.util.UUID
        public var propertyId: java.util.UUID
        public var totalAmount: Double?
        public var depositIncluded: Boolean?
        public var electricityNote: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          id: java.util.UUID,propertyId: java.util.UUID,
          block_: Builder.() -> Unit
        ): Variables {
          var id= id
            var propertyId= propertyId
            var totalAmount: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var depositIncluded: com.google.firebase.dataconnect.OptionalVariable<Boolean?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var electricityNote: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var id: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { id = value_ }
              
            override var propertyId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { propertyId = value_ }
              
            override var totalAmount: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { totalAmount = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var depositIncluded: Boolean?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { depositIncluded = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var electricityNote: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { electricityNote = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              id=id,propertyId=propertyId,totalAmount=totalAmount,depositIncluded=depositIncluded,electricityNote=electricityNote,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val bill_update: BillKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateBill"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateBillMutation.ref(
  
    id: java.util.UUID,propertyId: java.util.UUID,

  
    block_: UpdateBillMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateBillMutation.Data,
    UpdateBillMutation.Variables
  > =
  ref(
    
      UpdateBillMutation.Variables.build(
        id=id,propertyId=propertyId,
  
    block_
      )
    
  )

public suspend fun UpdateBillMutation.execute(

  
    
      id: java.util.UUID,propertyId: java.util.UUID,

  
    block_: UpdateBillMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateBillMutation.Data,
    UpdateBillMutation.Variables
  > =
  ref(
    
      id=id,propertyId=propertyId,
  
    block_
    
  ).execute()


