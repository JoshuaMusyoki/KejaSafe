
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



public interface BookListingMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      BookListingMutation.Data,
      BookListingMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val listingId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val landlordId: String,
    val guestName: String,
    val startDate: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val endDate: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
    val notes: String,
    val slotCount: Int
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingBooking_insert: ListingBookingKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "BookListing"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun BookListingMutation.ref(
  
    listingId: java.util.UUID,landlordId: String,guestName: String,startDate: com.google.firebase.Timestamp,endDate: com.google.firebase.Timestamp,notes: String,slotCount: Int,

  
  
): com.google.firebase.dataconnect.MutationRef<
    BookListingMutation.Data,
    BookListingMutation.Variables
  > =
  ref(
    
      BookListingMutation.Variables(
        listingId=listingId,landlordId=landlordId,guestName=guestName,startDate=startDate,endDate=endDate,notes=notes,slotCount=slotCount,
  
      )
    
  )

public suspend fun BookListingMutation.execute(

  
    
      listingId: java.util.UUID,landlordId: String,guestName: String,startDate: com.google.firebase.Timestamp,endDate: com.google.firebase.Timestamp,notes: String,slotCount: Int,

  

  ): com.google.firebase.dataconnect.MutationResult<
    BookListingMutation.Data,
    BookListingMutation.Variables
  > =
  ref(
    
      listingId=listingId,landlordId=landlordId,guestName=guestName,startDate=startDate,endDate=endDate,notes=notes,slotCount=slotCount,
  
    
  ).execute()


