
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


public interface BillsForLandlordListingsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      BillsForLandlordListingsQuery.Data,
      BillsForLandlordListingsQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val listingIds: List<@kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID>
  ) {
    
    
  }
  

  
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
    public val operationName: String = "BillsForLandlordListings"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun BillsForLandlordListingsQuery.ref(
  
    listingIds: List<java.util.UUID>,

  
  
): com.google.firebase.dataconnect.QueryRef<
    BillsForLandlordListingsQuery.Data,
    BillsForLandlordListingsQuery.Variables
  > =
  ref(
    
      BillsForLandlordListingsQuery.Variables(
        listingIds=listingIds,
  
      )
    
  )

public suspend fun BillsForLandlordListingsQuery.execute(

  
    
      listingIds: List<java.util.UUID>,

  

  ): com.google.firebase.dataconnect.QueryResult<
    BillsForLandlordListingsQuery.Data,
    BillsForLandlordListingsQuery.Variables
  > =
  ref(
    
      listingIds=listingIds,
  
    
  ).execute()


  public fun BillsForLandlordListingsQuery.flow(
    
      listingIds: List<java.util.UUID>,

  
    
    ): kotlinx.coroutines.flow.Flow<BillsForLandlordListingsQuery.Data> =
    ref(
        
          listingIds=listingIds,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

