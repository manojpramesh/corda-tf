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
object IOUSchema

/**
 * An IOUState schema.
 */
object IOUSchemaV1 : MappedSchema(
        schemaFamily = IOUSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentIOU::class.java)) {
    @Entity
    @Table(name = "po_states")
    class PersistentIOU(

            @Column(name = "data")
            var data: String,

            @Column(name = "status")
            var status: String,

            @Column(name = "id")
            var id: String,

            @Column(name = "lender")
            var lenderName: String,

            @Column(name = "borrower")
            var borrowerName: String

    ) : PersistentState()
}