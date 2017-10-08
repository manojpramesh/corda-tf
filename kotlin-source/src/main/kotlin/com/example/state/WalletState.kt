package com.example.state

import com.example.schema.WalletSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import java.util.*

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param user balance of the user.
 * @param seller balance of the seller.
 * @param bank balance of the seller.
 */
data class WalletState(val user: Int,
                       val seller: Int,
                       val bank: Int,
                       val lender:Party,
                       override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(lender)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is WalletSchemaV1 -> WalletSchemaV1.PersistentWallet(
                    this.user,
                    this.seller,
                    this.bank
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(WalletSchemaV1)
}