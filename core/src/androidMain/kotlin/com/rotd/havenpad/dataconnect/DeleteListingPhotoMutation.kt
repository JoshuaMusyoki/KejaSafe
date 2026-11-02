
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



public interface DeleteListingPhotoMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      DeleteListingPhotoMutation.Data,
      DeleteListingPhotoMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingPhoto_delete: ListingPhotoKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "DeleteListingPhoto"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun DeleteListingPhotoMutation.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    DeleteListingPhotoMutation.Data,
    DeleteListingPhotoMutation.Variables
  > =
  ref(
    
      DeleteListingPhotoMutation.Variables(
        id=id,
  
      )
    
  )

public suspend fun DeleteListingPhotoMutation.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    DeleteListingPhotoMutation.Data,
    DeleteListingPhotoMutation.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


