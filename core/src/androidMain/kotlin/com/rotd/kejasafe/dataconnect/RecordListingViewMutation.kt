
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



public interface RecordListingViewMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      RecordListingViewMutation.Data,
      RecordListingViewMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val listingId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingEvent_insert: ListingEventKey,
    val listing_update: ListingKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "RecordListingView"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun RecordListingViewMutation.ref(
  
    listingId: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    RecordListingViewMutation.Data,
    RecordListingViewMutation.Variables
  > =
  ref(
    
      RecordListingViewMutation.Variables(
        listingId=listingId,
  
      )
    
  )

public suspend fun RecordListingViewMutation.execute(

  
    
      listingId: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    RecordListingViewMutation.Data,
    RecordListingViewMutation.Variables
  > =
  ref(
    
      listingId=listingId,
  
    
  ).execute()


