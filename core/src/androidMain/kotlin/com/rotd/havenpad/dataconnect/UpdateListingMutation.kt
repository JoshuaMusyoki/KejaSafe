
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



public interface UpdateListingMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      UpdateListingMutation.Data,
      UpdateListingMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val address: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val pricePerMonth: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val proximity: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val university: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val propertyType: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val unitsCount: com.google.firebase.dataconnect.OptionalVariable<Int?>,
    val garbageCollection: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val water: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val electricityBilling: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val electricityMonthly: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val extraChargesJson: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val deposit: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val status: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var id: java.util.UUID
        public var title: String?
        public var address: String?
        public var pricePerMonth: Double?
        public var proximity: String?
        public var university: String?
        public var propertyType: String?
        public var unitsCount: Int?
        public var garbageCollection: Double?
        public var water: Double?
        public var electricityBilling: String?
        public var electricityMonthly: Double?
        public var extraChargesJson: String?
        public var deposit: Double?
        public var status: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          id: java.util.UUID,
          block_: Builder.() -> Unit
        ): Variables {
          var id= id
            var title: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var address: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var pricePerMonth: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var proximity: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var university: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var propertyType: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var unitsCount: com.google.firebase.dataconnect.OptionalVariable<Int?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var garbageCollection: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var water: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var electricityBilling: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var electricityMonthly: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var extraChargesJson: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var deposit: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var status: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var id: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { id = value_ }
              
            override var title: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { title = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var address: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { address = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var pricePerMonth: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { pricePerMonth = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var proximity: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { proximity = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var university: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { university = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var propertyType: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { propertyType = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var unitsCount: Int?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { unitsCount = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var garbageCollection: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { garbageCollection = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var water: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { water = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var electricityBilling: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { electricityBilling = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var electricityMonthly: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { electricityMonthly = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var extraChargesJson: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { extraChargesJson = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var deposit: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { deposit = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var status: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { status = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              id=id,title=title,address=address,pricePerMonth=pricePerMonth,proximity=proximity,university=university,propertyType=propertyType,unitsCount=unitsCount,garbageCollection=garbageCollection,water=water,electricityBilling=electricityBilling,electricityMonthly=electricityMonthly,extraChargesJson=extraChargesJson,deposit=deposit,status=status,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listing_update: ListingKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateListing"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateListingMutation.ref(
  
    id: java.util.UUID,

  
    block_: UpdateListingMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateListingMutation.Data,
    UpdateListingMutation.Variables
  > =
  ref(
    
      UpdateListingMutation.Variables.build(
        id=id,
  
    block_
      )
    
  )

public suspend fun UpdateListingMutation.execute(

  
    
      id: java.util.UUID,

  
    block_: UpdateListingMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateListingMutation.Data,
    UpdateListingMutation.Variables
  > =
  ref(
    
      id=id,
  
    block_
    
  ).execute()


