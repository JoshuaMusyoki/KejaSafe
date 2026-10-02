
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


public interface BrowseListingsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      BrowseListingsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listings: List<ListingsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ListingsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String,
    val address: String,
    val pricePerMonth: Double?,
    val currency: String,
    val status: String,
    val proximity: String?,
    val university: String?,
    val propertyType: String,
    val unitsCount: Int,
    val garbageCollection: Double,
    val water: Double,
    val electricityBilling: String,
    val electricityMonthly: Double,
    val extraChargesJson: String,
    val deposit: Double,
    val activeBookings: Int,
    val totalViews: Int,
    val inquiries: Int,
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val landlord: Landlord,
    val listingPhotos_on_listing: List<ListingPhotosOnListingItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Landlord(
  
    val id: String,
    val username: String,
    val fullName: String,
    val businessName: String?
  ) {
    
    
  }
      
        @kotlinx.serialization.Serializable
  public data class ListingPhotosOnListingItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val url: String,
    val sortOrder: Int
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "BrowseListings"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun BrowseListingsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    BrowseListingsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun BrowseListingsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    BrowseListingsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun BrowseListingsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<BrowseListingsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

