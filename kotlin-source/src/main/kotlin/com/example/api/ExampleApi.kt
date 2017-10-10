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
     * Displays all PO states that exist in the node's vault.
     */
    @GET
    @Path("orders")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPOs(): List<JSONObject> {
        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        val states =  vaultStates.states
        var result: MutableList<JSONObject> = mutableListOf()

        states.mapTo(result) {
            json{
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
     * Changes PO Status to Created
     */
    @POST
    @Path("createOrder")
    @Consumes(MediaType.APPLICATION_JSON)
    fun createPO(data: Order): Any {
        var status: Response.Status
        var msg: String

        var stat = "Created"
        var tradeId = UniqueIdentifier().id.toString()
        var orderNumber = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data.typeOfDocument, data.data, stat, orderNumber, tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle
                    .returnValue
                    .getOrThrow()
            status = Response.Status.CREATED
            msg = "{ 'txHash': ${result.id}, 'tradeId': $tradeId }"
            //val vaultStates = rpcOps.vaultQueryBy<IOUState>()
            //return vaultStates.states.last()

        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }

        return Response.status(status).entity(msg).build()
    }


    // TODO : Convert individual status change req to path based switch

    /**
     * Changes PO Status to Approved and don't need finance
     */
    @POST
    @Path("approve")
    @Consumes(MediaType.APPLICATION_JSON)
    fun ApprovePO(data: Order): Response {
        var status: Response.Status
        var msg: String
        var stat = "Approved"
        var orderNumber = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data.typeOfDocument, data.data, stat, orderNumber, data.tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            msg = "{'txHash': ${result.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
    }

    /**
     * Changes PO Status to Approved and don't need finance
     */
    @POST
    @Path("request-finance")
    @Consumes(MediaType.APPLICATION_JSON)
    fun NeedFinance(data: Order): Response {
        var status: Response.Status
        var msg: String
        var stat = "FinanceRequested"
        var orderNumber = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data.typeOfDocument, data.data, stat, orderNumber, data.tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            msg = "{'txHash': ${result.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
    }

    /**
     * Changes PO Status to Rejected
     */
    @POST
    @Path("reject")
    @Consumes(MediaType.APPLICATION_JSON)
    fun RejectPO(data: Order): Response {
        var status: Response.Status
        var msg: String
        var stat = "Rejected"
        var orderNumber = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data.typeOfDocument, data.data, stat, orderNumber, data.tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            msg = "{'txHash': ${result.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
    }

    /**
     * Changes PO Status to FinanceApproved
     */
    @POST
    @Path("approve-finance")
    @Consumes(MediaType.APPLICATION_JSON)
    fun ApproveFinancing(data: Order): Response {
        var status: Response.Status
        var msg: String
        var stat = "FinanceApproved"
        var orderNumber = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data.typeOfDocument, data.data, stat, orderNumber, data.tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            msg = "{'txHash': ${result.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
    }

    /**
     * Changes PO Status to FinanceRejected
     */
    @POST
    @Path("reject-finance")
    @Consumes(MediaType.APPLICATION_JSON)
    fun RejectFinancing(data: Order): Response {
        var status: Response.Status
        var msg: String
        val stat = "FinanceRejected"
        var orderNumber = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data.typeOfDocument, data.data, stat, orderNumber, data.tradeId)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            msg = "{'txHash': ${result.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
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
        var msg: String
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
            msg = "{'txHash': ${resultTo.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
    }

    /**
     * Load Balance to the given account
     */
    @POST
    @Path("onboardWallet")
    @Consumes(MediaType.APPLICATION_JSON)
    fun onboardWallet(data: WalletOnboarding): Response {

        if(data.value <= 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("Amount should be greater than 0\n").build()

        var status: Response.Status
        var msg: String
        try {
            val flowHandle = rpcOps.startTrackedFlow(::InitiatorWallet, data.entityMetadata, data.entityId, data.value)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle.returnValue.getOrThrow()
            status = Response.Status.CREATED
            msg = "{'txHash': ${result.id} }"
        } catch (ex: Throwable) {
            status = Response.Status.BAD_REQUEST
            msg = ex.message!!
            logger.error(msg, ex)
        }
        return Response.status(status).entity(msg).build()
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