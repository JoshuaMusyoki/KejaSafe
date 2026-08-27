
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



public interface RequestBillPaymentMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      RequestBillPaymentMutation.Data,
      RequestBillPaymentMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val bill_update: BillKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "RequestBillPayment"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun RequestBillPaymentMutation.ref(
  
    id: java.util.UUID,

  
  
): com.google.firebase.dataconnect.MutationRef<
    RequestBillPaymentMutation.Data,
    RequestBillPaymentMutation.Variables
  > =
  ref(
    
      RequestBillPaymentMutation.Variables(
        id=id,
  
      )
    
  )

public suspend fun RequestBillPaymentMutation.execute(

  
    
      id: java.util.UUID,

  

  ): com.google.firebase.dataconnect.MutationResult<
    RequestBillPaymentMutation.Data,
    RequestBillPaymentMutation.Variables
  > =
  ref(
    
      id=id,
  
    
  ).execute()


