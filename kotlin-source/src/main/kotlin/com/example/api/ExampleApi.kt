package com.example.api

import com.example.flow.ExampleFlow.Initiator
import com.example.state.IOUState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
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
    fun getPOById(@QueryParam("id") id: String): List<StateAndRef<IOUState>> {
        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        return vaultStates.states.filter { it.state.data.id == id }
    }

    /**
     * Displays PO details based on the id - DO NOT USE
     * TODO : Build a custom state query
     */
    @GET
    @Path("pobystatus")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPOByStatus(@QueryParam("status") status: String): List<StateAndRef<IOUState>> {
        val vaultStates = rpcOps.vaultQueryBy<IOUState>()
        return vaultStates.states.filter { it.state.data.status == status }
    }


    /**
     * Changes PO Status to Created
     */
    @PUT
    @Path("create-po")
    fun createPO(@FormParam("data") data: String): Response {
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
}