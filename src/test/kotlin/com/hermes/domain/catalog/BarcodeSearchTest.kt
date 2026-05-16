package com.hermes.domain.catalog

import com.hermes.domain.money.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarcodeSearchTest {
    @Test
    fun `finds product by exact normalized barcode`() {
        val result = BarcodeSearch.findByBarcode("786-100 1234567", listOf(item("item_1", "7861001234567")))

        assertEquals("item_1", result?.id)
    }

    @Test
    fun `ignores archived item in barcode search`() {
        val result = BarcodeSearch.findByBarcode(
            "7861001234567",
            listOf(item("item_1", "7861001234567", status = CatalogItemStatus.ARCHIVED)),
        )

        assertNull(result)
    }

    @Test
    fun `searches by text or sku`() {
        val items = listOf(
            item("item_1", "7861001234567", localName = "Coca Cola 500ml"),
            item("item_2", "7861007654321", localName = "Agua sin gas"),
        )

        assertEquals("item_1", BarcodeSearch.searchByCodeOrText("coca", items).single().id)
    }

    private fun item(
        id: String,
        code: String,
        status: CatalogItemStatus = CatalogItemStatus.ACTIVE,
        localName: String = "Coca Cola 500ml",
    ): OrganizationCatalogItem = OrganizationCatalogItem(
        id = id,
        organizationId = "org_1",
        branchId = "br_1",
        activityId = "act_1",
        templateId = "tpl_1",
        globalCatalogId = "global_1",
        localName = localName,
        searchableText = localName.lowercase(),
        type = CatalogItemType.PRODUCT,
        status = status,
        localPrice = Money.of("1.00"),
        taxProfileId = "taxp_iva_full_current",
        publicDiscoveryStatus = PublicDiscoveryStatus.PRIVATE,
        identifiers = listOf(
            CatalogIdentifier.create(
                type = CatalogIdentifierType.EAN_13,
                value = code,
                scope = CatalogIdentifierScope.GLOBAL,
                source = CatalogIdentifierSource.PLATFORM,
                status = CatalogIdentifierStatus.VERIFIED,
            ),
        ),
    )
}
