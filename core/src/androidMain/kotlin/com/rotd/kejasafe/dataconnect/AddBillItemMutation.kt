
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



public interface AddBillItemMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      KejasafeConnector,
      AddBillItemMutation.Data,
      AddBillItemMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val billId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val propertyId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val amount: Double
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val billItem_insert: BillItemKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "AddBillItem"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun AddBillItemMutation.ref(
  
    billId: java.util.UUID,propertyId: java.util.UUID,name: String,amount: Double,

  
  
): com.google.firebase.dataconnect.MutationRef<
    AddBillItemMutation.Data,
    AddBillItemMutation.Variables
  > =
  ref(
    
      AddBillItemMutation.Variables(
        billId=billId,propertyId=propertyId,name=name,amount=amount,
  
      )
    
  )

public suspend fun AddBillItemMutation.execute(

  
    
      billId: java.util.UUID,propertyId: java.util.UUID,name: String,amount: Double,

  

  ): com.google.firebase.dataconnect.MutationResult<
    AddBillItemMutation.Data,
    AddBillItemMutation.Variables
  > =
  ref(
    
      billId=billId,propertyId=propertyId,name=name,amount=amount,
  
    
  ).execute()


