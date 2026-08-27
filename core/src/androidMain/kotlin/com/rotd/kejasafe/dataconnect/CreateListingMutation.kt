
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



public interface CreateListingMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      CreateListingMutation.Data,
      CreateListingMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val title: String,
    val address: String,
    val pricePerMonth: com.google.firebase.dataconnect.OptionalVariable<Double?>,
    val proximity: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val university: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val propertyType: String,
    val unitsCount: Int,
    val garbageCollection: Double,
    val water: Double,
    val electricityBilling: String,
    val electricityMonthly: Double,
    val extraChargesJson: String,
    val deposit: Double
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var title: String
        public var address: String
        public var pricePerMonth: Double?
        public var proximity: String?
        public var university: String?
        public var propertyType: String
        public var unitsCount: Int
        public var garbageCollection: Double
        public var water: Double
        public var electricityBilling: String
        public var electricityMonthly: Double
        public var extraChargesJson: String
        public var deposit: Double
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          title: String,address: String,propertyType: String,unitsCount: Int,garbageCollection: Double,water: Double,electricityBilling: String,electricityMonthly: Double,extraChargesJson: String,deposit: Double,
          block_: Builder.() -> Unit
        ): Variables {
          var title= title
            var address= address
            var pricePerMonth: com.google.firebase.dataconnect.OptionalVariable<Double?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var proximity: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var university: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var propertyType= propertyType
            var unitsCount= unitsCount
            var garbageCollection= garbageCollection
            var water= water
            var electricityBilling= electricityBilling
            var electricityMonthly= electricityMonthly
            var extraChargesJson= extraChargesJson
            var deposit= deposit
            

          return object : Builder {
            override var title: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { title = value_ }
              
            override var address: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { address = value_ }
              
            override var pricePerMonth: Double?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { pricePerMonth = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var proximity: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { proximity = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var university: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { university = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var propertyType: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { propertyType = value_ }
              
            override var unitsCount: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { unitsCount = value_ }
              
            override var garbageCollection: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { garbageCollection = value_ }
              
            override var water: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { water = value_ }
              
            override var electricityBilling: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { electricityBilling = value_ }
              
            override var electricityMonthly: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { electricityMonthly = value_ }
              
            override var extraChargesJson: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { extraChargesJson = value_ }
              
            override var deposit: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { deposit = value_ }
              
            
          }.apply(block_)
          .let {
            Variables(
              title=title,address=address,pricePerMonth=pricePerMonth,proximity=proximity,university=university,propertyType=propertyType,unitsCount=unitsCount,garbageCollection=garbageCollection,water=water,electricityBilling=electricityBilling,electricityMonthly=electricityMonthly,extraChargesJson=extraChargesJson,deposit=deposit,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listing_insert: ListingKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateListing"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateListingMutation.ref(
  
    title: String,address: String,propertyType: String,unitsCount: Int,garbageCollection: Double,water: Double,electricityBilling: String,electricityMonthly: Double,extraChargesJson: String,deposit: Double,

  
    block_: CreateListingMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    CreateListingMutation.Data,
    CreateListingMutation.Variables
  > =
  ref(
    
      CreateListingMutation.Variables.build(
        title=title,address=address,propertyType=propertyType,unitsCount=unitsCount,garbageCollection=garbageCollection,water=water,electricityBilling=electricityBilling,electricityMonthly=electricityMonthly,extraChargesJson=extraChargesJson,deposit=deposit,
  
    block_
      )
    
  )

public suspend fun CreateListingMutation.execute(

  
    
      title: String,address: String,propertyType: String,unitsCount: Int,garbageCollection: Double,water: Double,electricityBilling: String,electricityMonthly: Double,extraChargesJson: String,deposit: Double,

  
    block_: CreateListingMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    CreateListingMutation.Data,
    CreateListingMutation.Variables
  > =
  ref(
    
      title=title,address=address,propertyType=propertyType,unitsCount=unitsCount,garbageCollection=garbageCollection,water=water,electricityBilling=electricityBilling,electricityMonthly=electricityMonthly,extraChargesJson=extraChargesJson,deposit=deposit,
  
    block_
    
  ).execute()


