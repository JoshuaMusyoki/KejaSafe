
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


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface MyBillsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      MyBillsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val bills: List<BillsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class BillsItem(
  
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
    val fullName: String
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
    public val operationName: String = "MyBills"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun MyBillsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    MyBillsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun MyBillsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    MyBillsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun MyBillsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<MyBillsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

