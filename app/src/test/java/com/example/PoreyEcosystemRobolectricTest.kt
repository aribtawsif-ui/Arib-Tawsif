package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PoreyDatabase
import com.example.data.repository.PoreyRepository
import com.example.domain.model.FulfilmentStatus
import com.example.domain.model.PaymentMethod
import com.example.domain.model.ReturnRequestType
import com.example.domain.model.ReturnStatus
import com.example.domain.model.StockMovementReason
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PoreyEcosystemRobolectricTest {

    private lateinit var db: PoreyDatabase
    private lateinit var repository: PoreyRepository

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PoreyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PoreyRepository(db)
        repository.ensureSeeded()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `pos webhook deduplicates idempotency keys and rejects invalid signatures`() = runTest {
        val listingBefore = db.poreyDao().getListingByMerchantSku("mer_banani_pos", "DL-PNJ-IVR-M")!!
        val startPhysical = listingBefore.physicalQty

        // Invalid signature must fail
        val badSig = repository.processPosSaleWebhook(
            actorRole = UserRole.PHYSICAL_MERCHANT,
            merchantId = "mer_banani_pos",
            sku = "DL-PNJ-IVR-M",
            qtySold = 1,
            idempotencyKey = "POS-KEY-BAD-1",
            signatureValid = false
        )
        assertTrue(badSig.isFailure)

        // Valid webhook decrements physical stock
        val ok = repository.processPosSaleWebhook(
            actorRole = UserRole.PHYSICAL_MERCHANT,
            merchantId = "mer_banani_pos",
            sku = "DL-PNJ-IVR-M",
            qtySold = 2,
            idempotencyKey = "POS-KEY-100",
            signatureValid = true
        )
        assertTrue(ok.isSuccess)

        // Duplicate idempotency key must NOT deduct twice
        val dup = repository.processPosSaleWebhook(
            actorRole = UserRole.PHYSICAL_MERCHANT,
            merchantId = "mer_banani_pos",
            sku = "DL-PNJ-IVR-M",
            qtySold = 2,
            idempotencyKey = "POS-KEY-100",
            signatureValid = true
        )
        assertTrue(dup.isSuccess)
        assertTrue(dup.getOrThrow().contains("Duplicate webhook ignored"))

        val listingAfter = db.poreyDao().getListingByMerchantSku("mer_banani_pos", "DL-PNJ-IVR-M")!!
        assertEquals(startPhysical - 2, listingAfter.physicalQty)
    }

    @Test
    fun `checkout reserves stock atomically and prevents overselling last unit`() = runTest {
        // lst_saree_banani starts with physical=2, reserved=0, available=2
        val add1 = repository.addToCart("usr_customer_1", "lst_saree_banani", 2)
        assertTrue(add1.isSuccess)

        // Adding a 3rd unit exceeds available stock
        val addOverflow = repository.addToCart("usr_customer_1", "lst_saree_banani", 1)
        assertTrue(addOverflow.isFailure)

        // Place order with POREY10 promo
        val orderRes = repository.placeOrder(
            customerId = "usr_customer_1",
            customerName = "Nafisa Rahman",
            deliveryAddress = "Road 11, Banani, Dhaka",
            zoneId = "zone_dhaka_express",
            paymentMethod = PaymentMethod.BKASH_SANDBOX,
            promoCodeInput = "POREY10"
        )
        assertTrue(orderRes.isSuccess)

        val listingAfterOrder = db.poreyDao().getListingById("lst_saree_banani")!!
        assertEquals(2, listingAfterOrder.physicalQty)
        assertEquals(2, listingAfterOrder.reservedQty)

        // Second customer tries to buy the already-reserved saree -> rejected
        val secondBuyerAdd = repository.addToCart("usr_customer_2", "lst_saree_banani", 1)
        assertTrue(secondBuyerAdd.isFailure)
    }

    @Test
    fun `end to end fulfilment delivery and cancellation release work accurately`() = runTest {
        // Order 1 unit of lst_panjabi_banani_l (physical=3, reserved=0)
        assertTrue(repository.addToCart("usr_customer_1", "lst_panjabi_banani_l", 1).isSuccess)
        val orderCode = repository.placeOrder(
            customerId = "usr_customer_1",
            customerName = "Nafisa Rahman",
            deliveryAddress = "Banani, Dhaka",
            zoneId = "zone_dhaka_express",
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            promoCodeInput = ""
        ).getOrThrow()

        val order = db.poreyDao().observeOrders().first().first { it.orderCode == orderCode }
        val ful = db.poreyDao().observeFulfilments().first().first { it.orderId == order.id }

        // Merchant confirms & readies
        assertTrue(repository.advanceFulfilmentStatus(UserRole.PHYSICAL_MERCHANT, ful.id, FulfilmentStatus.CONFIRMED_PREPARING).isSuccess)
        assertTrue(repository.advanceFulfilmentStatus(UserRole.PHYSICAL_MERCHANT, ful.id, FulfilmentStatus.READY_FOR_PICKUP).isSuccess)

        // Rider accepts, picks up, delivers with proof
        assertTrue(repository.advanceFulfilmentStatus(UserRole.RIDER, ful.id, FulfilmentStatus.RIDER_ASSIGNED, "usr_rider_1", "Sajib").isSuccess)
        assertTrue(repository.advanceFulfilmentStatus(UserRole.RIDER, ful.id, FulfilmentStatus.PICKED_UP, "usr_rider_1", "Sajib").isSuccess)
        assertTrue(repository.advanceFulfilmentStatus(UserRole.RIDER, ful.id, FulfilmentStatus.OUT_FOR_DELIVERY, "usr_rider_1", "Sajib").isSuccess)
        assertTrue(repository.advanceFulfilmentStatus(UserRole.RIDER, ful.id, FulfilmentStatus.DELIVERED, "usr_rider_1", "Sajib", "Signed by Nafisa").isSuccess)

        val listingDelivered = db.poreyDao().getListingById("lst_panjabi_banani_l")!!
        assertEquals(2, listingDelivered.physicalQty)
        assertEquals(0, listingDelivered.reservedQty)

        // Customer requests exchange -> Admin closes with refund
        assertTrue(repository.submitReturnOrExchange("usr_customer_1", ful.id, ReturnRequestType.EXCHANGE, "Need size XL").isSuccess)
        val ret = db.poreyDao().observeReturnRequests().first().first()
        assertTrue(repository.reviewReturnRequest(UserRole.ADMIN, ret.id, ReturnStatus.REFUNDED_CLOSED, "Approved").isSuccess)

        // Second order cancelled before prep must release reserved stock back to available
        assertTrue(repository.addToCart("usr_customer_1", "lst_panjabi_banani_l", 1).isSuccess)
        val cancelOrderCode = repository.placeOrder(
            customerId = "usr_customer_1",
            customerName = "Nafisa Rahman",
            deliveryAddress = "Banani, Dhaka",
            zoneId = "zone_dhaka_express",
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            promoCodeInput = ""
        ).getOrThrow()
        val cancelOrder = db.poreyDao().observeOrders().first().first { it.orderCode == cancelOrderCode }
        val cancelFul = db.poreyDao().observeFulfilments().first().first { it.orderId == cancelOrder.id }
        assertEquals(1, db.poreyDao().getListingById("lst_panjabi_banani_l")!!.reservedQty)

        assertTrue(repository.advanceFulfilmentStatus(UserRole.CUSTOMER, cancelFul.id, FulfilmentStatus.CANCELLED).isSuccess)
        val listingReleased = db.poreyDao().getListingById("lst_panjabi_banani_l")!!
        assertEquals(2, listingReleased.physicalQty)
        assertEquals(0, listingReleased.reservedQty)
    }

    @Test
    fun `rbac blocks unauthorized roles from admin governance and catalog publishing`() = runTest {
        val employeePublish = repository.createOnlineProductAndListing(
            actorRole = UserRole.STORE_EMPLOYEE,
            merchantId = "mer_banani_pos",
            name = "Unauthorized Shirt",
            brand = "Test",
            category = "Men",
            fabric = "Cotton",
            size = "M",
            color = "Cream",
            sku = "UNAUTH-1",
            priceBdt = 1500,
            discountPriceBdt = null,
            initialStock = 5,
            imageKey = "panjabi"
        )
        assertTrue(employeePublish.isFailure)

        val customerAdjust = repository.adjustManualStock(
            actorRole = UserRole.CUSTOMER,
            listingId = "lst_panjabi_mirpur_m",
            deltaPhysical = -1,
            deltaDamaged = 0,
            reason = StockMovementReason.MANUAL_SALE,
            note = "Unauthorized"
        )
        assertTrue(customerAdjust.isFailure)

        val merchantVerify = repository.toggleMerchantVerification(UserRole.ONLINE_MERCHANT, "mer_uttara_street")
        assertTrue(merchantVerify.isFailure)
    }

    @Test
    fun `guest cannot buy products until logging in or registering as customer`() = runTest {
        val guestAdd = repository.addToCart("guest_readonly", "lst_saree_atelier", 1, isGuest = true)
        assertTrue(guestAdd.isFailure)

        val guestOrder = repository.placeOrder(
            customerId = "guest_readonly",
            customerName = "Guest",
            deliveryAddress = "Banani, Dhaka",
            zoneId = "zone_dhaka_express",
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            promoCodeInput = "",
            isGuest = true
        )
        assertTrue(guestOrder.isFailure)

        // Register a new customer and log in
        val regRes = repository.registerCustomer(
            name = "Tahmid Chowdhury",
            phone = "+880 1711-998877",
            email = "tahmid@example.bd",
            password = "secret123",
            address = "Road 4, Gulshan 1, Dhaka"
        )
        assertTrue(regRes.isSuccess)
        val loginRes = repository.loginUser("tahmid@example.bd", "secret123")
        assertTrue(loginRes.isSuccess)
        val customer = loginRes.getOrThrow()

        assertTrue(repository.addToCart(customer.id, "lst_saree_atelier", 1, isGuest = false).isSuccess)
        val orderRes = repository.placeOrder(
            customerId = customer.id,
            customerName = customer.name,
            deliveryAddress = customer.defaultAddress,
            zoneId = "zone_dhaka_express",
            paymentMethod = PaymentMethod.CASH_ON_DELIVERY,
            promoCodeInput = "",
            isGuest = false
        )
        assertTrue(orderRes.isSuccess)
    }

    @Test
    fun `shop owner registration stays unverified until admin verifies and grants verified badge`() = runTest {
        val regShop = repository.registerShopOwner(
            ownerName = "Mahmudul Haque",
            phone = "+880 1811-223344",
            email = "gulshan.silk@example.bd",
            password = "shoppass",
            shopName = "Gulshan Silk Loom",
            area = "Gulshan 1, Dhaka",
            address = "House 12, Road 23, Gulshan 1",
            storeType = com.example.domain.model.StoreType.ONLINE_ONLY,
            posProvider = null,
            tradeLicenseRef = "DNCC-TRAD-55110"
        )
        assertTrue(regShop.isSuccess)
        val (ownerUser, createdMerchant) = regShop.getOrThrow()
        assertEquals(false, createdMerchant.isVerified)

        // Unverified shop cannot publish products yet
        val blockedPublish = repository.createOnlineProductAndListing(
            actorRole = UserRole.ONLINE_MERCHANT,
            merchantId = createdMerchant.id,
            name = "Gulshan Silk Scarf",
            brand = "Gulshan Silk",
            category = "Women",
            fabric = "Pure Silk",
            size = "Free Size",
            color = "Sage",
            sku = "GSL-SCF-01",
            priceBdt = 1850,
            discountPriceBdt = null,
            initialStock = 5,
            imageKey = "saree"
        )
        assertTrue(blockedPublish.isFailure)

        // Admin verifies the shop -> Verified badge granted & publishing unlocked
        assertTrue(repository.toggleMerchantVerification(UserRole.ADMIN, createdMerchant.id).isSuccess)
        val verifiedMerchant = db.poreyDao().getMerchantById(createdMerchant.id)!!
        assertEquals(true, verifiedMerchant.isVerified)

        val allowedPublish = repository.createOnlineProductAndListing(
            actorRole = UserRole.ONLINE_MERCHANT,
            merchantId = createdMerchant.id,
            name = "Gulshan Silk Scarf",
            brand = "Gulshan Silk",
            category = "Women",
            fabric = "Pure Silk",
            size = "Free Size",
            color = "Sage",
            sku = "GSL-SCF-01",
            priceBdt = 1850,
            discountPriceBdt = null,
            initialStock = 5,
            imageKey = "saree"
        )
        assertTrue(allowedPublish.isSuccess)
        assertEquals("usr_shop_", ownerUser.id.take(9))
    }

    @Test
    fun `web view storefront html reflects dark light modes and verified shop badges`() = runTest {
        val merchants = db.poreyDao().observeMerchants().first()
        val products = db.poreyDao().observeProducts().first()
        val listings = db.poreyDao().observeListings().first()
        val merchantsById = merchants.associateBy { it.id }

        val catalogViews = products.map { p ->
            val offers = repository.buildRankedOffers(listings.filter { it.productId == p.id }, merchantsById)
            com.example.domain.model.CatalogProductView(
                product = p,
                offers = offers,
                lowestPriceBdt = offers.minOfOrNull { it.effectivePriceBdt } ?: 0,
                originalMaxPriceBdt = offers.maxOfOrNull { it.listing.priceBdt } ?: 0,
                totalAvailableQty = offers.sumOf { it.availableQty },
                nearestMerchantArea = "Banani, Dhaka",
                nearestDistanceKm = 1.4,
                hasExpressOption = true,
                bestOffer = offers.firstOrNull()
            )
        }

        val lightHtml = com.example.ui.screens.buildPoreyWebHtml(
            com.example.ui.state.PoreyUiState(
                isReady = true,
                isDarkMode = false,
                activeRole = UserRole.GUEST,
                merchants = merchants,
                catalogProducts = catalogViews
            )
        )
        assertTrue(lightHtml.contains("#F3EFE4"))
        assertTrue(lightHtml.contains("Verified"))
        assertTrue(lightHtml.contains("Pending Admin KYC"))
        assertTrue(lightHtml.contains("Guest Mode"))

        val darkHtml = com.example.ui.screens.buildPoreyWebHtml(
            com.example.ui.state.PoreyUiState(
                isReady = true,
                isDarkMode = true,
                activeRole = UserRole.CUSTOMER,
                merchants = merchants,
                catalogProducts = catalogViews
            )
        )
        assertTrue(darkHtml.contains("#141A16"))
        assertTrue(darkHtml.contains("Switch to Light Mode"))

        val stateJson = com.example.ui.screens.buildPoreyWebsiteStateJson(
            com.example.ui.state.PoreyUiState(
                isReady = true,
                isDarkMode = true,
                isGuest = false,
                activeRole = UserRole.CUSTOMER,
                merchants = merchants,
                catalogProducts = catalogViews
            )
        )
        assertTrue(stateJson.contains("\"isDarkMode\":true"))
        assertTrue(stateJson.contains("\"isGuest\":false"))
        assertTrue(stateJson.contains("\"isVerified\":true"))
        assertTrue(stateJson.contains("\"products\""))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val assetHtml = context.assets.open("website/index.html").bufferedReader().use { it.readText() }
        assertTrue(assetHtml.contains("Porey - পরে"))
        assertTrue(assetHtml.contains("app.js"))
    }
}
