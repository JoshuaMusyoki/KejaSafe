
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


public interface GetUserByUsernameQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      KejasafeConnector,
      GetUserByUsernameQuery.Data,
      GetUserByUsernameQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val username: String
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val users: List<UsersItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class UsersItem(
  
    val id: String,
    val email: String,
    val username: String
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetUserByUsername"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetUserByUsernameQuery.ref(
  
    username: String,

  
  
): com.google.firebase.dataconnect.QueryRef<
    GetUserByUsernameQuery.Data,
    GetUserByUsernameQuery.Variables
  > =
  ref(
    
      GetUserByUsernameQuery.Variables(
        username=username,
  
      )
    
  )

public suspend fun GetUserByUsernameQuery.execute(

  
    
      username: String,

  

  ): com.google.firebase.dataconnect.QueryResult<
    GetUserByUsernameQuery.Data,
    GetUserByUsernameQuery.Variables
  > =
  ref(
    
      username=username,
  
    
  ).execute()


  public fun GetUserByUsernameQuery.flow(
    
      username: String,

  
    
    ): kotlinx.coroutines.flow.Flow<GetUserByUsernameQuery.Data> =
    ref(
        
          username=username,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

