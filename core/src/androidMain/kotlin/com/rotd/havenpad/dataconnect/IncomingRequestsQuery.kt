
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


public interface IncomingRequestsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      IncomingRequestsQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingRequests: List<ListingRequestsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class ListingRequestsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val requesterName: String,
    val type: String,
    val propertyTitle: String,
    val detail: String,
    val timeLabel: String,
    val createdAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val listing: Listing
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class Listing(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val title: String
  ) {
    
    
  }
      
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "IncomingRequests"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun IncomingRequestsQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    IncomingRequestsQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun IncomingRequestsQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    IncomingRequestsQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun IncomingRequestsQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<IncomingRequestsQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

