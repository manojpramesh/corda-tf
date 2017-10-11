package com.example.api

import com.example.flow.ExampleFlow.Initiator
import com.example.flow.WalletFlow.InitiatorWallet
import com.example.state.IOUState
import com.example.state.WalletState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.json.simple.JSONObject
import org.slf4j.Logger
import java.util.*
import javax.json.Json
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val NOTARY_NAME = "Controller"
val NETWORK_MAP_NAME = "Network Map Service"

// This API is accessible from /api/example. All paths specified below are relative to it.
@Path("trade")
class ExampleApi(val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ExampleApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                .filter { it != myLegalName && it.organisation != NOTARY_NAME && it.organisation != NETWORK_MAP_NAME })
    }


    /**
     * Get all order logs
     */
    @GET
    @Path("orderlogs")
    @Produces(MediaType.APPLICATION_JSON)
    fun orderlogs(): Any {
        val states = rpcOps.vaultQueryBy<IOUState>().states
        var result: MutableList<JSONObject> = mutableListOf()
        states.mapTo(result) {
            json {
                "typeOfDocument" To it.state.data.typeOfDocument
                "data" To it.state.data.data
                "tradeId" To it.state.data.tradeId
                "orderNumber" To it.state.data.orderNumber
                "status" To it.state.data.status
            }
        }
        return result
    }

    /**
     * Filter orders based on trade id
     */
    @GET
    @Path("orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPOs(@QueryParam("tradeId") tradeId: String): List<JSONObject> {
        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        val states =  vaultStates.states
        var result: MutableList<JSONObject> = mutableListOf()

        for ((state) in states){
            if(state.data.tradeId == tradeId){
                result.add(json {
                    "typeOfDocument" To state.data.typeOfDocument
                    "data" To state.data.data
                    "tradeId" To state.data.tradeId
                    "orderId" To state.data.orderNumber
                    "status" To state.data.status
                })
            }
        }
        return result
    }



    /**
     * Create Document
     */
    @POST
    @Path("create/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createOrder(data: Order, @PathParam("type") type: String): Any {

        val stat = "Created"
        var tradeId = data.tradeId
        val orderNumber = UniqueIdentifier().id.toString()
        var typeOfDocument: String

        when(type.toLowerCase()){
            "purchaseorder" -> {
                tradeId =  UniqueIdentifier().id.toString()
                typeOfDocument = "Purchase Order"
            }
            "salesorder" -> typeOfDocument = "Sales Order"
            "billoflading" -> typeOfDocument = "Bill of Lading"
            "shipmentorder" -> typeOfDocument = "Shipment Order"
            "financeorder" -> typeOfDocument = "Finance Order"
            "customsclearanceorder" -> typeOfDocument = "Customs Clearance Order"
            else -> return Response.status(Response.Status.NOT_FOUND).build()
        }

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            val msg = json {
                "txHash" To result.id
                "tradeId" To tradeId
                "orderId" To orderNumber
            }
            return Response.ok(msg, MediaType.APPLICATION_JSON).build()
        } catch (ex: Throwable) {
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }
    }


    /**
     * Changes order Status to Approved
     */
    @POST
    @Path("approve/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun Approve(data: Order, @PathParam("type") type: String): Response {
        val stat = "Approved"
        var tradeId = data.tradeId
        val orderNumber = UniqueIdentifier().id.toString()

        var typeOfDocument: String = when(type.toLowerCase()){
            "purchaseorder" -> "Purchase Order"
            "salesorder" -> "Sales Order"
            "billoflading" -> "Bill of Lading"
            "shipmentorder" -> "Shipment Order"
            "financeorder" -> "Finance Order"
            "customsclearanceorder" -> "Customs Clearance Order"
            else -> return Response.status(Response.Status.BAD_REQUEST).entity("Not a valid document").build()
        }

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            val msg = json {
                "txHash" To result.id
                "tradeId" To tradeId
                "orderId" To orderNumber
            }
            return Response.ok(msg, MediaType.APPLICATION_JSON).build()
        } catch (ex: Throwable) {
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }
    }

    /**
     * Changes Order Status to Rejected
     */
    @POST
    @Path("reject/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun Reject(data: Order, @PathParam("type") type: String): Response {
        val stat = "Rejected"
        var tradeId = data.tradeId
        val orderNumber = UniqueIdentifier().id.toString()

        var typeOfDocument: String = when(type.toLowerCase()){
            "purchaseorder" -> "Purchase Order"
            "salesorder" -> "Sales Order"
            "billoflading" -> "Bill of Lading"
            "shipmentorder" -> "Shipment Order"
            "financeorder" -> "Finance Order"
            "customsclearanceorder" -> "Customs Clearance Order"
            else -> return Response.status(Response.Status.BAD_REQUEST).entity("Not a valid document").build()
        }

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            val msg = json {
                "txHash" To result.id
                "tradeId" To tradeId
                "orderId" To orderNumber
            }
            return Response.ok(msg, MediaType.APPLICATION_JSON).build()
        } catch (ex: Throwable) {
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }
    }

    /**
     * Changes order Status to Finance Requested
     */
    @POST
    @Path("requestFinance/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun NeedFinance(data: Order, @PathParam("type") type: String): Response {
        val stat = "Finance Requested"
        var tradeId = data.tradeId
        val orderNumber = UniqueIdentifier().id.toString()

        var typeOfDocument: String = when(type.toLowerCase()){
            "purchaseorder" -> "Purchase Order"
            "salesorder" -> "Sales Order"
            else -> return Response.status(Response.Status.NOT_FOUND).build()
        }

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            val msg = json {
                "txHash" To result.id
                "tradeId" To tradeId
                "orderId" To orderNumber
            }
            return Response.ok(msg, MediaType.APPLICATION_JSON).build()
        } catch (ex: Throwable) {
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }
    }

    /**
     * Changes order Status to FinanceApproved
     */
    @POST
    @Path("approveFinance/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun ApproveFinancing(data: Order, @PathParam("type") type: String): Response {
        val stat = "Finance Approved"
        var tradeId = data.tradeId
        val orderNumber = UniqueIdentifier().id.toString()

        var typeOfDocument: String = when(type.toLowerCase()){
            "purchaseorder" -> "Purchase Order"
            "salesorder" -> "Sales Order"
            else -> return Response.status(Response.Status.NOT_FOUND).build()
        }

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            val msg = json {
                "txHash" To result.id
                "tradeId" To tradeId
                "orderId" To orderNumber
            }
            return Response.ok(msg, MediaType.APPLICATION_JSON).build()
        } catch (ex: Throwable) {
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }
    }

    /**
     * Changes order Status to FinanceRejected
     */
    @POST
    @Path("rejectFinance/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun RejectFinancing(data: Order, @PathParam("type") type: String): Response {
        val stat = "Finance Rejected"
        var tradeId = data.tradeId
        val orderNumber = UniqueIdentifier().id.toString()

        var typeOfDocument: String = when(type.toLowerCase()){
            "purchaseorder" -> "Purchase Order"
            "salesorder" -> "Sales Order"
            else -> return Response.status(Response.Status.NOT_FOUND).build()
        }

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            val msg = json {
                "txHash" To result.id
                "tradeId" To tradeId
                "orderId" To orderNumber
            }
            return Response.ok(msg, MediaType.APPLICATION_JSON).build()
        } catch (ex: Throwable) {
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build()
        }
    }









    /**
     * Get Current balance
     */
    @GET
    @Path("getWallet")
    @Produces(MediaType.APPLICATION_JSON)
    fun getWallet(@QueryParam("id") id: Int): Any? {
        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        val wallet = vaultStates.states

        var result: StateAndRef<WalletState>? = null

        for (n in wallet.count() downTo 1) {
            if(wallet[n - 1].state.data.entityId == id)
                result = wallet[n - 1]
            if(result != null) break
        }

        if(result != null) return json {
            "entityId" To result?.state?.data?.entityId
            "entityMetadata" To result?.state?.data?.entityMetadata
            "value" To result?.state?.data?.value
        }

        return null
    }


    /**
     * Get wallet logs
    */
    @GET
    @Path("getWalletTransactionLogs")
    @Produces(MediaType.APPLICATION_JSON)
    fun getWalletTransactionLogs(): List<JSONObject> {
        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        val states: List<StateAndRef<WalletState>> = vaultStates.states
        var result: MutableList<JSONObject> = mutableListOf()

        states.mapTo(result) {
            json {
                "entityMetadata" To it.state.data.entityMetadata
                "entityId" To it.state.data.entityId
                "value" To it.state.data.value
            }
        }

        return result
    }


    /**
     * Transfer amount from one account to another
     */
    @POST
    @Path("transfer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun Transfer(data: WalletTransfer): Response {

        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        val wallet = vaultStates.states
        var fromWallet: StateAndRef<WalletState>? = null
        var toWallet: StateAndRef<WalletState>? = null

        for (n in wallet.count() downTo 1) {
            if(wallet[n - 1].state.data.entityId == data.from)
                fromWallet = wallet[n - 1]
            if(wallet[n - 1].state.data.entityId == data.to)
                toWallet = wallet[n - 1]
            if(fromWallet != null && toWallet != null)
                break
        }

        if(fromWallet == null && toWallet == null)
            return Response.status(Response.Status.BAD_REQUEST).entity("User id not valid\n").build()


        var status: Response.Status
        try {

            val fromWalletMeta = fromWallet?.state?.data?.entityMetadata ?: ""
            val fromWalletAmount = (fromWallet?.state?.data?.value ?: 0) - data.value
            val flowHandleFrom = rpcOps.startTrackedFlow(::InitiatorWallet, fromWalletMeta, data.from, fromWalletAmount)
            flowHandleFrom.progress.subscribe { println(">> $it") }
            flowHandleFrom.returnValue.getOrThrow()

            val toWalletMeta = toWallet?.state?.data?.entityMetadata ?: ""
            val toWalletAmount = (toWallet?.state?.data?.value ?: 0) + data.value
            val flowHandleTo = rpcOps.startTrackedFlow(::InitiatorWallet, toWalletMeta, data.to, toWalletAmount)
            flowHandleTo.progress.subscribe { println(">> $it") }
            val resultTo = flowHandleTo.returnValue.getOrThrow()

            status = Response.Status.CREATED
            val msg = json {
                "txHash" To resultTo.id
            }
            return Response.status(status).entity(msg).build()
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(status).entity(msg).build()
        }

    }

    /**
     * Load Balance to the given account
     */
    @POST
    @Path("onboardWallet")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun onboardWallet(data: WalletOnboarding): Response {

        if(data.value <= 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("Amount should be greater than 0\n").build()

        var status: Response.Status
        try {
            val flowHandle = rpcOps.startTrackedFlow(::InitiatorWallet, data.entityMetadata, data.entityId, data.value)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            val msg = json {
                "txHash" To result.id
            }
            return Response.status(status).entity(msg).build()
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            val msg = ex.message!!
            logger.error(msg, ex)
            return Response.status(status).entity(msg).build()
        }
    }






    fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
        return JsonObjectBuilder().json(build)
    }

    class JsonObjectBuilder {
        private val deque: Deque<JSONObject> = ArrayDeque()

        fun json(build: JsonObjectBuilder.() -> Unit): JSONObject {
            deque.push(JSONObject())
            this.build()
            return deque.pop()
        }

        infix fun <T> String.To(value: T) {
            deque.peek().put(this, value)
        }
    }

}

class WalletOnboarding {
    val entityMetadata: String = ""
    val entityId: Int = 0
    val value: Int = 0
}

class WalletTransfer {
    val from: Int = 0
    val to: Int = 0
    val value: Int = 0
}

class Order {
    val typeOfDocument: String = ""
    val data: String = ""
    val tradeId: String = ""

}