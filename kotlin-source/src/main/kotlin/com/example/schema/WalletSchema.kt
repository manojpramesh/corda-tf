package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object WalletSchema

/**
 * An IOUState schema.
 */
object WalletSchemaV1 : MappedSchema(
        schemaFamily = IOUSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentWallet::class.java)) {
    @Entity
    @Table(name = "po_states")
    class PersistentWallet(

            @Column(name = "user")
            var user: Int,

            @Column(name = "seller")
            var seller: Int,

            @Column(name = "bank")
            var bank: Int

    ) : PersistentState()
}