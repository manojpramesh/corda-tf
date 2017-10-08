package com.example.api

import com.example.flow.ExampleFlow.Initiator
import com.example.flow.WalletFlow.InitiatorWallet
import com.example.state.IOUState
import com.example.state.WalletState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import java.util.*
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
    @Path("pos")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPOs(): List<StateAndRef<IOUState>> {
        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        return vaultStates.states
    }


    /**
     * Displays PO details based on the id - DO NOT USE
     * TODO : Build a custom state query
     */
    @GET
    @Path("pobyid")
    @Produces(MediaType.APPLICATION_JSON)
    fun POById(@QueryParam("id") id: Int): List<StateAndRef<IOUState>> {

        //val linearStateCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        //val results = vaultService.queryBy<LinearState>(linearStateCriteria)

        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        return vaultStates.states
    }

    /**
     * Displays PO details based on the id - DO NOT USE
     * TODO : Build a custom state query
     */
    @GET
    @Path("pobystatus")
    @Produces(MediaType.APPLICATION_JSON)
    fun POByStatus(@QueryParam("status") status: String): List<StateAndRef<IOUState>> {
        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        return vaultStates.states.filter { it.state.data.status == status }
    }


    /**
     * Changes PO Status to Created
     */
    @PUT
    @Path("create-po")
    fun createPO(@FormParam("data") data: String): Any {
        var status: Response.Status
        var msg: String
        var stat = "Created"
        var id = UniqueIdentifier().id.toString()

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data, stat, id)
            flowHandle.progress.subscribe { println(">> $it") }
            val result = flowHandle
                    .returnValue
                    .getOrThrow()
            status = Response.Status.CREATED
            msg = "{ 'txHash': ${result.id}, 'id': $id }"
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
    @PUT
    @Path("approve-po")
    fun ApprovePO(@FormParam("data") data: String,
                  @FormParam("id") id: String): Response {
        var status: Response.Status
        var msg: String
        var stat = "Approved"

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data, stat, id)
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
    @PUT
    @Path("request-finance")
    fun NeedFinance(@FormParam("data") data: String,
                  @FormParam("id") id: String): Response {
        var status: Response.Status
        var msg: String
        var stat = "FinanceRequested"

        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data, stat, id)
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
    @PUT
    @Path("reject-po")
    fun RejectPO(@FormParam("data") data: String,
                  @FormParam("id") id: String): Response {
        var status: Response.Status
        var msg: String
        var stat = "Rejected"
        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data, stat, id)
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
    @PUT
    @Path("approve-finance")
    fun ApproveFinancing(@FormParam("data") data: String,
                 @FormParam("id") id: String): Response {
        var status: Response.Status
        var msg: String
        var stat = "FinanceApproved"
        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data, stat, id)
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
    @PUT
    @Path("reject-finance")
    fun RejectFinancing(@FormParam("data") data: String,
                 @FormParam("id") id: String): Response {
        var status: Response.Status
        var msg: String
        var stat = "FinanceRejected"
        try {
            val flowHandle = rpcOps.startTrackedFlow(::Initiator, data, stat, id)
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
    @Path("getBalance")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBalance(): StateAndRef<WalletState> {
        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        return vaultStates.states.last()
    }


    /**
     * Get wallet logs
     */
    @GET
    @Path("wallet-logs")
    @Produces(MediaType.APPLICATION_JSON)
    fun getBalanceLogs(): List<StateAndRef<WalletState>> {
        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        return vaultStates.states
    }


    /**
     * Transfer amount from one account to another
     */
    @PUT
    @Path("transfer-amount")
    fun Transfer(@FormParam("from") from: String,
                 @FormParam("to") to: String,
                 @FormParam("amount") amount: Int): Response {

        if(amount <= 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("Amount should be greater than 0\n").build()

        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        val balance = vaultStates.states.last().state.data
        var user = balance.user
        var seller = balance.seller
        var bank = balance.bank

        if(from != "user" && from != "seller" && from != "bank")
            return Response.status(Response.Status.BAD_REQUEST).entity("from user not valid\n").build()

        if(to != "user" && to != "seller" && to != "bank")
            return Response.status(Response.Status.BAD_REQUEST).entity("to user not valid\n").build()

        when (from) {
            "user" -> user -= amount
            "seller" -> seller -= amount
            "bank" -> bank -= amount
        }

        when (to) {
            "user" -> user += amount
            "seller" -> seller += amount
            "bank" -> bank += amount
        }

        var status: Response.Status
        var msg: String
        try {
            val flowHandle = rpcOps.startTrackedFlow(::InitiatorWallet,user, seller, bank)
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
     * Load Balance to the given account
     */
    @PUT
    @Path("load-balance")
    fun LoadBalance(@FormParam("to") to: String,
                 @FormParam("amount") amount: Int): Response {

        if(amount <= 0)
            return Response.status(Response.Status.BAD_REQUEST).entity("Amount should be greater than 0\n").build()

        val vaultStates = rpcOps.vaultQueryBy<WalletState>()
        val balance = vaultStates.states.last().state.data
        var user = balance.user
        var seller = balance.seller
        var bank = balance.bank

        when (to) {
            "user" -> user += amount
            "seller" -> seller += amount
            "bank" -> bank += amount
            else -> return Response.status(Response.Status.BAD_REQUEST).entity("Not a valid user\n").build()
        }

        var status: Response.Status
        var msg: String
        try {
            val flowHandle = rpcOps.startTrackedFlow(::InitiatorWallet, user, seller, bank)
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
}