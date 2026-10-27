
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


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface GetBillQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      GetBillQuery.Data,
      GetBillQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val bill: Bill?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Bill(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val propertyId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val propertyTitle: String,
    val tenantName: String,
    val bookingId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID?,
    val month: Int,
    val year: Int,
    val totalAmount: Double,
    val dueDate: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val status: String,
    val depositIncluded: Boolean,
    val electricityNote: String?,
    val paymentRequested: Boolean,
    val user: User,
    val billItems_on_bill: List<BillItemsOnBillItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class User(
  
    val id: String,
    val fullName: String,
    val email: String
  ) {
    
    
  }
      
        @kotlinx.serialization.Serializable
  public data class BillItemsOnBillItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val amount: Double
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetBill"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetBillQuery.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetBillQuery.Data,
    GetBillQuery.Variables
  > =
  ref(
    
      GetBillQuery.Variables(
        id=id,
  
      )
    
  )

public suspend fun GetBillQuery.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetBillQuery.Data,
    GetBillQuery.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


  public fun GetBillQuery.flow(
    
      id: java.util.UUID,

  
    
    ): kotlinx.coroutines.flow.Flow<GetBillQuery.Data> =
    ref(
        
          id=id,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

