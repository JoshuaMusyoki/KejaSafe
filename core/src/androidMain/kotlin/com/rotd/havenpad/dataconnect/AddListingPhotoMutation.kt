
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



public interface AddListingPhotoMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      AddListingPhotoMutation.Data,
      AddListingPhotoMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val listingId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val url: String,
    val sortOrder: Int
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingPhoto_insert: ListingPhotoKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "AddListingPhoto"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun AddListingPhotoMutation.ref(
  
    listingId: java.util.UUID,url: String,sortOrder: Int,

  
  
): com.google.firebase.dataconnect.MutationRef<
    AddListingPhotoMutation.Data,
    AddListingPhotoMutation.Variables
  > =
  ref(
    
      AddListingPhotoMutation.Variables(
        listingId=listingId,url=url,sortOrder=sortOrder,
  
      )
    
  )

public suspend fun AddListingPhotoMutation.execute(

  
    
      listingId: java.util.UUID,url: String,sortOrder: Int,

  

  ): com.google.firebase.dataconnect.MutationResult<
    AddListingPhotoMutation.Data,
    AddListingPhotoMutation.Variables
  > =
  ref(
    
      listingId=listingId,url=url,sortOrder=sortOrder,
  
    
  ).execute()


