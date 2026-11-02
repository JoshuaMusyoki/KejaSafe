
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


public interface GetListingQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      GetListingQuery.Data,
      GetListingQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listing: Listing?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Listing(
  
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
    val businessName: String?,
    val phone: String?
  ) {
    
    
  }
      
        @kotlinx.serialization.Serializable
  public data class ListingPhotosOnListingItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val url: String,
    val sortOrder: Int,
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetListing"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetListingQuery.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetListingQuery.Data,
    GetListingQuery.Variables
  > =
  ref(
    
      GetListingQuery.Variables(
        id=id,
  
      )
    
  )

public suspend fun GetListingQuery.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetListingQuery.Data,
    GetListingQuery.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


  public fun GetListingQuery.flow(
    
      id: java.util.UUID,

  
    
    ): kotlinx.coroutines.flow.Flow<GetListingQuery.Data> =
    ref(
        
          id=id,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

