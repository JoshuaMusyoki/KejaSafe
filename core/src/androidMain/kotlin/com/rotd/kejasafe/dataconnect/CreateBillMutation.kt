
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



public interface CreateBillMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      CreateBillMutation.Data,
      CreateBillMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: String,
    val propertyId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val propertyTitle: String,
    val tenantName: String,
    val bookingId: com.google.firebase.dataconnect.OptionalVariable<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID?>,
    val month: Int,
    val year: Int,
    val totalAmount: Double,
    val dueDate: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val depositIncluded: Boolean,
    val electricityNote: com.google.firebase.dataconnect.OptionalVariable<String?>
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var userId: String
        public var propertyId: java.util.UUID
        public var propertyTitle: String
        public var tenantName: String
        public var bookingId: java.util.UUID?
        public var month: Int
        public var year: Int
        public var totalAmount: Double
        public var dueDate: com.google.firebase.Timestamp
        public var depositIncluded: Boolean
        public var electricityNote: String?
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          userId: String,propertyId: java.util.UUID,propertyTitle: String,tenantName: String,month: Int,year: Int,totalAmount: Double,dueDate: com.google.firebase.Timestamp,depositIncluded: Boolean,
          block_: Builder.() -> Unit
        ): Variables {
          var userId= userId
            var propertyId= propertyId
            var propertyTitle= propertyTitle
            var tenantName= tenantName
            var bookingId: com.google.firebase.dataconnect.OptionalVariable<java.util.UUID?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var month= month
            var year= year
            var totalAmount= totalAmount
            var dueDate= dueDate
            var depositIncluded= depositIncluded
            var electricityNote: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            

          return object : Builder {
            override var userId: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { userId = value_ }
              
            override var propertyId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { propertyId = value_ }
              
            override var propertyTitle: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { propertyTitle = value_ }
              
            override var tenantName: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { tenantName = value_ }
              
            override var bookingId: java.util.UUID?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { bookingId = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var month: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { month = value_ }
              
            override var year: Int
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { year = value_ }
              
            override var totalAmount: Double
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { totalAmount = value_ }
              
            override var dueDate: com.google.firebase.Timestamp
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { dueDate = value_ }
              
            override var depositIncluded: Boolean
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { depositIncluded = value_ }
              
            override var electricityNote: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { electricityNote = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            
          }.apply(block_)
          .let {
            Variables(
              userId=userId,propertyId=propertyId,propertyTitle=propertyTitle,tenantName=tenantName,bookingId=bookingId,month=month,year=year,totalAmount=totalAmount,dueDate=dueDate,depositIncluded=depositIncluded,electricityNote=electricityNote,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val bill_insert: BillKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateBill"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateBillMutation.ref(
  
    userId: String,propertyId: java.util.UUID,propertyTitle: String,tenantName: String,month: Int,year: Int,totalAmount: Double,dueDate: com.google.firebase.Timestamp,depositIncluded: Boolean,

  
    block_: CreateBillMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    CreateBillMutation.Data,
    CreateBillMutation.Variables
  > =
  ref(
    
      CreateBillMutation.Variables.build(
        userId=userId,propertyId=propertyId,propertyTitle=propertyTitle,tenantName=tenantName,month=month,year=year,totalAmount=totalAmount,dueDate=dueDate,depositIncluded=depositIncluded,
  
    block_
      )
    
  )

public suspend fun CreateBillMutation.execute(

  
    
      userId: String,propertyId: java.util.UUID,propertyTitle: String,tenantName: String,month: Int,year: Int,totalAmount: Double,dueDate: com.google.firebase.Timestamp,depositIncluded: Boolean,

  
    block_: CreateBillMutation.Variables.Builder.() -> Unit = {}

  ): com.google.firebase.dataconnect.MutationResult<
    CreateBillMutation.Data,
    CreateBillMutation.Variables
  > =
  ref(
    
      userId=userId,propertyId=propertyId,propertyTitle=propertyTitle,tenantName=tenantName,month=month,year=year,totalAmount=totalAmount,dueDate=dueDate,depositIncluded=depositIncluded,
  
    block_
    
  ).execute()


