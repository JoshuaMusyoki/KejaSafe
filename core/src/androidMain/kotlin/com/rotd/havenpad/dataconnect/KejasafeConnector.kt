
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

import com.google.firebase.dataconnect.getInstance as _fdcGetInstance
import kotlin.time.Duration.Companion.milliseconds as _milliseconds

public interface KejasafeConnector : com.google.firebase.dataconnect.generated.GeneratedConnector<KejasafeConnector> {
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect

  
    public val addBillItem: AddBillItemMutation
  
    public val addListingPhoto: AddListingPhotoMutation
  
    public val approveBooking: ApproveBookingMutation
  
    public val billsForLandlordListings: BillsForLandlordListingsQuery
  
    public val bookListing: BookListingMutation
  
    public val browseListings: BrowseListingsQuery
  
    public val conversationMessages: ConversationMessagesQuery
  
    public val createBill: CreateBillMutation
  
    public val createConversation: CreateConversationMutation
  
    public val createListing: CreateListingMutation
  
    public val declineBooking: DeclineBookingMutation
  
    public val deleteListingPhoto: DeleteListingPhotoMutation
  
    public val getBill: GetBillQuery
  
    public val getListing: GetListingQuery
  
    public val getMyUser: GetMyUserQuery
  
    public val getUserByUsername: GetUserByUsernameQuery
  
    public val incomingRequests: IncomingRequestsQuery
  
    public val joinConversation: JoinConversationMutation
  
    public val landlordBookings: LandlordBookingsQuery
  
    public val myBills: MyBillsQuery
  
    public val myBookings: MyBookingsQuery
  
    public val myConversations: MyConversationsQuery
  
    public val myListings: MyListingsQuery
  
    public val payBill: PayBillMutation
  
    public val recordListingView: RecordListingViewMutation
  
    public val requestBillPayment: RequestBillPaymentMutation
  
    public val sendMessage: SendMessageMutation
  
    public val submitKyc: SubmitKycMutation
  
    public val updateBill: UpdateBillMutation
  
    public val updateListing: UpdateListingMutation
  
    public val updateMyProfile: UpdateMyProfileMutation
  
    public val upsertMyUser: UpsertMyUserMutation
  

  public companion object {
    @Suppress("MemberVisibilityCanBePrivate")
    public val config: com.google.firebase.dataconnect.ConnectorConfig = com.google.firebase.dataconnect.ConnectorConfig(
      connector = "havenpad",
      location = "us-central1",
      serviceId = "havenpad",
    )

    public fun getInstance(
      dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
    ):KejasafeConnector = synchronized(instances) {
      instances.getOrPut(dataConnect) {
        KejasafeConnectorImpl(dataConnect)
      }
    }

    private val instances = java.util.WeakHashMap<com.google.firebase.dataconnect.FirebaseDataConnect, KejasafeConnectorImpl>()

    
  }
}

public val KejasafeConnector.Companion.instance:KejasafeConnector
  get() = getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(
    config
  ))

public fun KejasafeConnector.Companion.getInstance(
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):KejasafeConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(config, settings))

public fun KejasafeConnector.Companion.getInstance(
  app: com.google.firebase.FirebaseApp,
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):KejasafeConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(app, config, settings))

private class KejasafeConnectorImpl(
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
) : KejasafeConnector {
  
    override val addBillItem by lazy(LazyThreadSafetyMode.PUBLICATION) {
      AddBillItemMutationImpl(this)
    }
  
    override val addListingPhoto by lazy(LazyThreadSafetyMode.PUBLICATION) {
      AddListingPhotoMutationImpl(this)
    }
  
    override val approveBooking by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ApproveBookingMutationImpl(this)
    }
  
    override val billsForLandlordListings by lazy(LazyThreadSafetyMode.PUBLICATION) {
      BillsForLandlordListingsQueryImpl(this)
    }
  
    override val bookListing by lazy(LazyThreadSafetyMode.PUBLICATION) {
      BookListingMutationImpl(this)
    }
  
    override val browseListings by lazy(LazyThreadSafetyMode.PUBLICATION) {
      BrowseListingsQueryImpl(this)
    }
  
    override val conversationMessages by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ConversationMessagesQueryImpl(this)
    }
  
    override val createBill by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateBillMutationImpl(this)
    }
  
    override val createConversation by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateConversationMutationImpl(this)
    }
  
    override val createListing by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateListingMutationImpl(this)
    }
  
    override val declineBooking by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeclineBookingMutationImpl(this)
    }
  
    override val deleteListingPhoto by lazy(LazyThreadSafetyMode.PUBLICATION) {
      DeleteListingPhotoMutationImpl(this)
    }
  
    override val getBill by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetBillQueryImpl(this)
    }
  
    override val getListing by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetListingQueryImpl(this)
    }
  
    override val getMyUser by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetMyUserQueryImpl(this)
    }
  
    override val getUserByUsername by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetUserByUsernameQueryImpl(this)
    }
  
    override val incomingRequests by lazy(LazyThreadSafetyMode.PUBLICATION) {
      IncomingRequestsQueryImpl(this)
    }
  
    override val joinConversation by lazy(LazyThreadSafetyMode.PUBLICATION) {
      JoinConversationMutationImpl(this)
    }
  
    override val landlordBookings by lazy(LazyThreadSafetyMode.PUBLICATION) {
      LandlordBookingsQueryImpl(this)
    }
  
    override val myBills by lazy(LazyThreadSafetyMode.PUBLICATION) {
      MyBillsQueryImpl(this)
    }
  
    override val myBookings by lazy(LazyThreadSafetyMode.PUBLICATION) {
      MyBookingsQueryImpl(this)
    }
  
    override val myConversations by lazy(LazyThreadSafetyMode.PUBLICATION) {
      MyConversationsQueryImpl(this)
    }
  
    override val myListings by lazy(LazyThreadSafetyMode.PUBLICATION) {
      MyListingsQueryImpl(this)
    }
  
    override val payBill by lazy(LazyThreadSafetyMode.PUBLICATION) {
      PayBillMutationImpl(this)
    }
  
    override val recordListingView by lazy(LazyThreadSafetyMode.PUBLICATION) {
      RecordListingViewMutationImpl(this)
    }
  
    override val requestBillPayment by lazy(LazyThreadSafetyMode.PUBLICATION) {
      RequestBillPaymentMutationImpl(this)
    }
  
    override val sendMessage by lazy(LazyThreadSafetyMode.PUBLICATION) {
      SendMessageMutationImpl(this)
    }
  
    override val submitKyc by lazy(LazyThreadSafetyMode.PUBLICATION) {
      SubmitKycMutationImpl(this)
    }
  
    override val updateBill by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateBillMutationImpl(this)
    }
  
    override val updateListing by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateListingMutationImpl(this)
    }
  
    override val updateMyProfile by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateMyProfileMutationImpl(this)
    }
  
    override val upsertMyUser by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpsertMyUserMutationImpl(this)
    }
  

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun operations(): List<com.google.firebase.dataconnect.generated.GeneratedOperation<KejasafeConnector, *, *>> =
    queries() + mutations()

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun mutations(): List<com.google.firebase.dataconnect.generated.GeneratedMutation<KejasafeConnector, *, *>> =
    listOf(
      addBillItem,
        addListingPhoto,
        approveBooking,
        bookListing,
        createBill,
        createConversation,
        createListing,
        declineBooking,
        deleteListingPhoto,
        joinConversation,
        payBill,
        recordListingView,
        requestBillPayment,
        sendMessage,
        submitKyc,
        updateBill,
        updateListing,
        updateMyProfile,
        upsertMyUser,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun queries(): List<com.google.firebase.dataconnect.generated.GeneratedQuery<KejasafeConnector, *, *>> =
    listOf(
      billsForLandlordListings,
        browseListings,
        conversationMessages,
        getBill,
        getListing,
        getMyUser,
        getUserByUsername,
        incomingRequests,
        landlordBookings,
        myBills,
        myBookings,
        myConversations,
        myListings,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect) =
    KejasafeConnectorImpl(dataConnect)

  override fun equals(other: Any?): Boolean =
    other is KejasafeConnectorImpl &&
    other.dataConnect == dataConnect

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "KejasafeConnectorImpl",
      dataConnect,
    )

  override fun toString(): String =
    "KejasafeConnectorImpl(dataConnect=$dataConnect)"
}



private open class KejasafeConnectorGeneratedQueryImpl<Data, Variables>(
  override val connector: KejasafeConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedQuery<KejasafeConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: KejasafeConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    KejasafeConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    KejasafeConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    KejasafeConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is KejasafeConnectorGeneratedQueryImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "KejasafeConnectorGeneratedQueryImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "KejasafeConnectorGeneratedQueryImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}

private open class KejasafeConnectorGeneratedMutationImpl<Data, Variables>(
  override val connector: KejasafeConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedMutation<KejasafeConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: KejasafeConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    KejasafeConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    KejasafeConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    KejasafeConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is KejasafeConnectorGeneratedMutationImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "KejasafeConnectorGeneratedMutationImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "KejasafeConnectorGeneratedMutationImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}



private class AddBillItemMutationImpl(
  connector: KejasafeConnector
):
  AddBillItemMutation,
  KejasafeConnectorGeneratedMutationImpl<
      AddBillItemMutation.Data,
      AddBillItemMutation.Variables
  >(
    connector,
    AddBillItemMutation.Companion.operationName,
    AddBillItemMutation.Companion.dataDeserializer,
    AddBillItemMutation.Companion.variablesSerializer,
  )


private class AddListingPhotoMutationImpl(
  connector: KejasafeConnector
):
  AddListingPhotoMutation,
  KejasafeConnectorGeneratedMutationImpl<
      AddListingPhotoMutation.Data,
      AddListingPhotoMutation.Variables
  >(
    connector,
    AddListingPhotoMutation.Companion.operationName,
    AddListingPhotoMutation.Companion.dataDeserializer,
    AddListingPhotoMutation.Companion.variablesSerializer,
  )


private class ApproveBookingMutationImpl(
  connector: KejasafeConnector
):
  ApproveBookingMutation,
  KejasafeConnectorGeneratedMutationImpl<
      ApproveBookingMutation.Data,
      ApproveBookingMutation.Variables
  >(
    connector,
    ApproveBookingMutation.Companion.operationName,
    ApproveBookingMutation.Companion.dataDeserializer,
    ApproveBookingMutation.Companion.variablesSerializer,
  )


private class BillsForLandlordListingsQueryImpl(
  connector: KejasafeConnector
):
  BillsForLandlordListingsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      BillsForLandlordListingsQuery.Data,
      BillsForLandlordListingsQuery.Variables
  >(
    connector,
    BillsForLandlordListingsQuery.Companion.operationName,
    BillsForLandlordListingsQuery.Companion.dataDeserializer,
    BillsForLandlordListingsQuery.Companion.variablesSerializer,
  )


private class BookListingMutationImpl(
  connector: KejasafeConnector
):
  BookListingMutation,
  KejasafeConnectorGeneratedMutationImpl<
      BookListingMutation.Data,
      BookListingMutation.Variables
  >(
    connector,
    BookListingMutation.Companion.operationName,
    BookListingMutation.Companion.dataDeserializer,
    BookListingMutation.Companion.variablesSerializer,
  )


private class BrowseListingsQueryImpl(
  connector: KejasafeConnector
):
  BrowseListingsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      BrowseListingsQuery.Data,
      Unit
  >(
    connector,
    BrowseListingsQuery.Companion.operationName,
    BrowseListingsQuery.Companion.dataDeserializer,
    BrowseListingsQuery.Companion.variablesSerializer,
  )


private class ConversationMessagesQueryImpl(
  connector: KejasafeConnector
):
  ConversationMessagesQuery,
  KejasafeConnectorGeneratedQueryImpl<
      ConversationMessagesQuery.Data,
      ConversationMessagesQuery.Variables
  >(
    connector,
    ConversationMessagesQuery.Companion.operationName,
    ConversationMessagesQuery.Companion.dataDeserializer,
    ConversationMessagesQuery.Companion.variablesSerializer,
  )


private class CreateBillMutationImpl(
  connector: KejasafeConnector
):
  CreateBillMutation,
  KejasafeConnectorGeneratedMutationImpl<
      CreateBillMutation.Data,
      CreateBillMutation.Variables
  >(
    connector,
    CreateBillMutation.Companion.operationName,
    CreateBillMutation.Companion.dataDeserializer,
    CreateBillMutation.Companion.variablesSerializer,
  )


private class CreateConversationMutationImpl(
  connector: KejasafeConnector
):
  CreateConversationMutation,
  KejasafeConnectorGeneratedMutationImpl<
      CreateConversationMutation.Data,
      CreateConversationMutation.Variables
  >(
    connector,
    CreateConversationMutation.Companion.operationName,
    CreateConversationMutation.Companion.dataDeserializer,
    CreateConversationMutation.Companion.variablesSerializer,
  )


private class CreateListingMutationImpl(
  connector: KejasafeConnector
):
  CreateListingMutation,
  KejasafeConnectorGeneratedMutationImpl<
      CreateListingMutation.Data,
      CreateListingMutation.Variables
  >(
    connector,
    CreateListingMutation.Companion.operationName,
    CreateListingMutation.Companion.dataDeserializer,
    CreateListingMutation.Companion.variablesSerializer,
  )


private class DeclineBookingMutationImpl(
  connector: KejasafeConnector
):
  DeclineBookingMutation,
  KejasafeConnectorGeneratedMutationImpl<
      DeclineBookingMutation.Data,
      DeclineBookingMutation.Variables
  >(
    connector,
    DeclineBookingMutation.Companion.operationName,
    DeclineBookingMutation.Companion.dataDeserializer,
    DeclineBookingMutation.Companion.variablesSerializer,
  )


private class DeleteListingPhotoMutationImpl(
  connector: KejasafeConnector
):
  DeleteListingPhotoMutation,
  KejasafeConnectorGeneratedMutationImpl<
      DeleteListingPhotoMutation.Data,
      DeleteListingPhotoMutation.Variables
  >(
    connector,
    DeleteListingPhotoMutation.Companion.operationName,
    DeleteListingPhotoMutation.Companion.dataDeserializer,
    DeleteListingPhotoMutation.Companion.variablesSerializer,
  )


private class GetBillQueryImpl(
  connector: KejasafeConnector
):
  GetBillQuery,
  KejasafeConnectorGeneratedQueryImpl<
      GetBillQuery.Data,
      GetBillQuery.Variables
  >(
    connector,
    GetBillQuery.Companion.operationName,
    GetBillQuery.Companion.dataDeserializer,
    GetBillQuery.Companion.variablesSerializer,
  )


private class GetListingQueryImpl(
  connector: KejasafeConnector
):
  GetListingQuery,
  KejasafeConnectorGeneratedQueryImpl<
      GetListingQuery.Data,
      GetListingQuery.Variables
  >(
    connector,
    GetListingQuery.Companion.operationName,
    GetListingQuery.Companion.dataDeserializer,
    GetListingQuery.Companion.variablesSerializer,
  )


private class GetMyUserQueryImpl(
  connector: KejasafeConnector
):
  GetMyUserQuery,
  KejasafeConnectorGeneratedQueryImpl<
      GetMyUserQuery.Data,
      Unit
  >(
    connector,
    GetMyUserQuery.Companion.operationName,
    GetMyUserQuery.Companion.dataDeserializer,
    GetMyUserQuery.Companion.variablesSerializer,
  )


private class GetUserByUsernameQueryImpl(
  connector: KejasafeConnector
):
  GetUserByUsernameQuery,
  KejasafeConnectorGeneratedQueryImpl<
      GetUserByUsernameQuery.Data,
      GetUserByUsernameQuery.Variables
  >(
    connector,
    GetUserByUsernameQuery.Companion.operationName,
    GetUserByUsernameQuery.Companion.dataDeserializer,
    GetUserByUsernameQuery.Companion.variablesSerializer,
  )


private class IncomingRequestsQueryImpl(
  connector: KejasafeConnector
):
  IncomingRequestsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      IncomingRequestsQuery.Data,
      Unit
  >(
    connector,
    IncomingRequestsQuery.Companion.operationName,
    IncomingRequestsQuery.Companion.dataDeserializer,
    IncomingRequestsQuery.Companion.variablesSerializer,
  )


private class JoinConversationMutationImpl(
  connector: KejasafeConnector
):
  JoinConversationMutation,
  KejasafeConnectorGeneratedMutationImpl<
      JoinConversationMutation.Data,
      JoinConversationMutation.Variables
  >(
    connector,
    JoinConversationMutation.Companion.operationName,
    JoinConversationMutation.Companion.dataDeserializer,
    JoinConversationMutation.Companion.variablesSerializer,
  )


private class LandlordBookingsQueryImpl(
  connector: KejasafeConnector
):
  LandlordBookingsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      LandlordBookingsQuery.Data,
      Unit
  >(
    connector,
    LandlordBookingsQuery.Companion.operationName,
    LandlordBookingsQuery.Companion.dataDeserializer,
    LandlordBookingsQuery.Companion.variablesSerializer,
  )


private class MyBillsQueryImpl(
  connector: KejasafeConnector
):
  MyBillsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      MyBillsQuery.Data,
      Unit
  >(
    connector,
    MyBillsQuery.Companion.operationName,
    MyBillsQuery.Companion.dataDeserializer,
    MyBillsQuery.Companion.variablesSerializer,
  )


private class MyBookingsQueryImpl(
  connector: KejasafeConnector
):
  MyBookingsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      MyBookingsQuery.Data,
      Unit
  >(
    connector,
    MyBookingsQuery.Companion.operationName,
    MyBookingsQuery.Companion.dataDeserializer,
    MyBookingsQuery.Companion.variablesSerializer,
  )


private class MyConversationsQueryImpl(
  connector: KejasafeConnector
):
  MyConversationsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      MyConversationsQuery.Data,
      Unit
  >(
    connector,
    MyConversationsQuery.Companion.operationName,
    MyConversationsQuery.Companion.dataDeserializer,
    MyConversationsQuery.Companion.variablesSerializer,
  )


private class MyListingsQueryImpl(
  connector: KejasafeConnector
):
  MyListingsQuery,
  KejasafeConnectorGeneratedQueryImpl<
      MyListingsQuery.Data,
      Unit
  >(
    connector,
    MyListingsQuery.Companion.operationName,
    MyListingsQuery.Companion.dataDeserializer,
    MyListingsQuery.Companion.variablesSerializer,
  )


private class PayBillMutationImpl(
  connector: KejasafeConnector
):
  PayBillMutation,
  KejasafeConnectorGeneratedMutationImpl<
      PayBillMutation.Data,
      PayBillMutation.Variables
  >(
    connector,
    PayBillMutation.Companion.operationName,
    PayBillMutation.Companion.dataDeserializer,
    PayBillMutation.Companion.variablesSerializer,
  )


private class RecordListingViewMutationImpl(
  connector: KejasafeConnector
):
  RecordListingViewMutation,
  KejasafeConnectorGeneratedMutationImpl<
      RecordListingViewMutation.Data,
      RecordListingViewMutation.Variables
  >(
    connector,
    RecordListingViewMutation.Companion.operationName,
    RecordListingViewMutation.Companion.dataDeserializer,
    RecordListingViewMutation.Companion.variablesSerializer,
  )


private class RequestBillPaymentMutationImpl(
  connector: KejasafeConnector
):
  RequestBillPaymentMutation,
  KejasafeConnectorGeneratedMutationImpl<
      RequestBillPaymentMutation.Data,
      RequestBillPaymentMutation.Variables
  >(
    connector,
    RequestBillPaymentMutation.Companion.operationName,
    RequestBillPaymentMutation.Companion.dataDeserializer,
    RequestBillPaymentMutation.Companion.variablesSerializer,
  )


private class SendMessageMutationImpl(
  connector: KejasafeConnector
):
  SendMessageMutation,
  KejasafeConnectorGeneratedMutationImpl<
      SendMessageMutation.Data,
      SendMessageMutation.Variables
  >(
    connector,
    SendMessageMutation.Companion.operationName,
    SendMessageMutation.Companion.dataDeserializer,
    SendMessageMutation.Companion.variablesSerializer,
  )


private class SubmitKycMutationImpl(
  connector: KejasafeConnector
):
  SubmitKycMutation,
  KejasafeConnectorGeneratedMutationImpl<
      SubmitKycMutation.Data,
      SubmitKycMutation.Variables
  >(
    connector,
    SubmitKycMutation.Companion.operationName,
    SubmitKycMutation.Companion.dataDeserializer,
    SubmitKycMutation.Companion.variablesSerializer,
  )


private class UpdateBillMutationImpl(
  connector: KejasafeConnector
):
  UpdateBillMutation,
  KejasafeConnectorGeneratedMutationImpl<
      UpdateBillMutation.Data,
      UpdateBillMutation.Variables
  >(
    connector,
    UpdateBillMutation.Companion.operationName,
    UpdateBillMutation.Companion.dataDeserializer,
    UpdateBillMutation.Companion.variablesSerializer,
  )


private class UpdateListingMutationImpl(
  connector: KejasafeConnector
):
  UpdateListingMutation,
  KejasafeConnectorGeneratedMutationImpl<
      UpdateListingMutation.Data,
      UpdateListingMutation.Variables
  >(
    connector,
    UpdateListingMutation.Companion.operationName,
    UpdateListingMutation.Companion.dataDeserializer,
    UpdateListingMutation.Companion.variablesSerializer,
  )


private class UpdateMyProfileMutationImpl(
  connector: KejasafeConnector
):
  UpdateMyProfileMutation,
  KejasafeConnectorGeneratedMutationImpl<
      UpdateMyProfileMutation.Data,
      UpdateMyProfileMutation.Variables
  >(
    connector,
    UpdateMyProfileMutation.Companion.operationName,
    UpdateMyProfileMutation.Companion.dataDeserializer,
    UpdateMyProfileMutation.Companion.variablesSerializer,
  )


private class UpsertMyUserMutationImpl(
  connector: KejasafeConnector
):
  UpsertMyUserMutation,
  KejasafeConnectorGeneratedMutationImpl<
      UpsertMyUserMutation.Data,
      UpsertMyUserMutation.Variables
  >(
    connector,
    UpsertMyUserMutation.Companion.operationName,
    UpsertMyUserMutation.Companion.dataDeserializer,
    UpsertMyUserMutation.Companion.variablesSerializer,
  )


