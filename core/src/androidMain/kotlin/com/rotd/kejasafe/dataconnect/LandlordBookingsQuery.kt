
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


public interface LandlordBookingsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      LandlordBookingsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingBookings: List<ListingBookingsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ListingBookingsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val guestName: String,
    val startDate: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val endDate: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val status: String,
    val notes: String,
    val slotCount: Int,
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val guestUser: GuestUser?,
    val listing: Listing
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class GuestUser(
  
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?
  ) {
    
    
  }
      
        @kotlinx.serialization.Serializable
  public data class Listing(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "LandlordBookings"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun LandlordBookingsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    LandlordBookingsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun LandlordBookingsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    LandlordBookingsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun LandlordBookingsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<LandlordBookingsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

