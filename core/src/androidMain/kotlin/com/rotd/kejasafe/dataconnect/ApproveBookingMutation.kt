
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



public interface ApproveBookingMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      ApproveBookingMutation.Data,
      ApproveBookingMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val listingBooking_update: ListingBookingKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "ApproveBooking"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ApproveBookingMutation.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    ApproveBookingMutation.Data,
    ApproveBookingMutation.Variables
  > =
  ref(
    
      ApproveBookingMutation.Variables(
        id=id,
  
      )
    
  )

public suspend fun ApproveBookingMutation.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    ApproveBookingMutation.Data,
    ApproveBookingMutation.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


