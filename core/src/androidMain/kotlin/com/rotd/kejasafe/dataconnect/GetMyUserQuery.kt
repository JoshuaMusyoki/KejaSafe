
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


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface GetMyUserQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      GetMyUserQuery.Data,
      Unit
    >
{
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val user: User?
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class User(
  
    val id: String,
    val email: String,
    val username: String,
    val fullName: String,
    val role: String,
    val kycStatus: String,
    val profilePictureUrl: String?,
    val googleId: String?,
    val major: String?,
    val yearOfStudy: Int?,
    val roommatePreferences: String?,
    val phone: String?,
    val businessName: String?
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetMyUser"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Unit> =
      kotlinx.serialization.serializer()
  }
}

public fun GetMyUserQuery.ref(
  
): com.google.firebase.dataconnect.QueryRef<
    GetMyUserQuery.Data,
    Unit
  > =
  ref(
    
      Unit
    
  )

public suspend fun GetMyUserQuery.execute(

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetMyUserQuery.Data,
    Unit
  > =
  ref(
    
  ).execute()


  public fun GetMyUserQuery.flow(
    
    ): kotlinx.coroutines.flow.Flow<GetMyUserQuery.Data> =
    ref(
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

