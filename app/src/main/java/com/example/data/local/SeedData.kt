package com.example.data.local

import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.DeliveryZoneEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.MerchantListingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.SavedAddressEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.domain.model.InventorySource
import com.example.domain.model.PosConnectionStatus
import com.example.domain.model.StockConfidence
import com.example.domain.model.StockMovementReason
import com.example.domain.model.StoreType
import com.example.domain.model.UserRole

/**
 * Clearly labelled development & sandbox seed dataset for Bangladesh fashion ecosystem.
 * External POS, MFS (bKash/Nagad), and courier connections are explicitly scoped as Sandbox Adapters.
 */
object SeedData {
    private val now = System.currentTimeMillis()
    private const val HOUR_MS = 3_600_000L

    val demoUsers = listOf(
        UserAccountEntity(
            id = "usr_customer_1",
            name = "Nafisa Rahman",
            phone = "+880 1711-440219",
            email = "nafisa.dhaka@example.bd",
            role = UserRole.CUSTOMER.name,
            defaultAddress = "House 24, Road 11, Banani, Dhaka 1213",
            defaultZone = "Dhaka Metro Express"
        ),
        UserAccountEntity(
            id = "usr_pos_merchant",
            name = "Tariq Hasan (Aarong Heritage Banani)",
            phone = "+880 1819-203100",
            email = "banani.pos@example.bd",
            role = UserRole.PHYSICAL_MERCHANT.name,
            assignedMerchantId = "mer_banani_pos",
            defaultAddress = "Kemal Ataturk Ave, Banani, Dhaka",
            defaultZone = "Dhaka Metro Express"
        ),
        UserAccountEntity(
            id = "usr_nopos_merchant",
            name = "Karim Mia (Mirpur Stitch Studio)",
            phone = "+880 1912-884201",
            email = "mirpur.stitch@example.bd",
            role = UserRole.NO_POS_MERCHANT.name,
            assignedMerchantId = "mer_mirpur_manual",
            defaultAddress = "Mirpur 10 Roundabout, Dhaka",
            defaultZone = "Dhaka Outer Standard"
        ),
        UserAccountEntity(
            id = "usr_online_merchant",
            name = "Zara Kabir (Dhaka Loom Studio)",
            phone = "+880 1755-610992",
            email = "studio@dhakaloom.example.bd",
            role = UserRole.ONLINE_MERCHANT.name,
            assignedMerchantId = "mer_dhaka_loom_online",
            defaultAddress = "Gulshan 2 Dispatch Hub, Dhaka",
            defaultZone = "Dhaka Metro Express"
        ),
        UserAccountEntity(
            id = "usr_employee_1",
            name = "Rashedul Islam (Floor Staff)",
            phone = "+880 1670-332190",
            email = "staff.banani@example.bd",
            role = UserRole.STORE_EMPLOYEE.name,
            assignedMerchantId = "mer_banani_pos",
            defaultAddress = "Banani, Dhaka",
            defaultZone = "Dhaka Metro Express"
        ),
        UserAccountEntity(
            id = "usr_rider_1",
            name = "Sajib Ahmed (Porey Fleet #14)",
            phone = "+880 1798-551023",
            email = "rider.sajib@example.bd",
            role = UserRole.RIDER.name,
            defaultAddress = "Mohakhali Rider Hub, Dhaka",
            defaultZone = "Dhaka Metro Express"
        ),
        UserAccountEntity(
            id = "usr_admin_1",
            name = "Farhana Yasmin (Ops Admin)",
            phone = "+880 1700-100200",
            email = "ops@porey.example.bd",
            role = UserRole.ADMIN.name,
            defaultAddress = "Porey HQ, Karwan Bazar, Dhaka",
            defaultZone = "Dhaka Metro Express"
        ),
        UserAccountEntity(
            id = "usr_super_admin",
            name = "Arib Tawsif (Super Admin)",
            phone = "+880 1700-000001",
            email = "superadmin@porey.example.bd",
            role = UserRole.SUPER_ADMIN.name,
            defaultAddress = "Porey HQ, Karwan Bazar, Dhaka",
            defaultZone = "Dhaka Metro Express"
        )
    )

    val demoMerchants = listOf(
        MerchantEntity(
            id = "mer_banani_pos",
            name = "Banani Heritage Boutique",
            storeType = StoreType.PHYSICAL_POS.name,
            area = "Banani, Dhaka",
            address = "Block E, Road 11, Banani, Dhaka 1213",
            distanceKm = 1.4,
            rating = 4.9,
            fulfilmentReliability = 0.98,
            avgPrepMinutes = 20,
            deliveryChargeBdt = 60,
            posProvider = "RetailBD Cloud POS",
            posStatus = PosConnectionStatus.CONNECTED_SANDBOX.name,
            commissionRatePct = 10,
            isVerified = true,
            isOpenNow = true,
            ownerUserId = "usr_pos_merchant",
            tradeLicenseRef = "DNCC-TRAD-004192",
            lastSyncedAt = now - 180_000L
        ),
        MerchantEntity(
            id = "mer_dhanmondi_pos",
            name = "Dhanmondi Loom House",
            storeType = StoreType.PHYSICAL_POS.name,
            area = "Dhanmondi 27, Dhaka",
            address = "Satmasjid Road, Dhanmondi, Dhaka 1209",
            distanceKm = 4.8,
            rating = 4.7,
            fulfilmentReliability = 0.94,
            avgPrepMinutes = 25,
            deliveryChargeBdt = 70,
            posProvider = "ShopUp Sync POS",
            posStatus = PosConnectionStatus.CONNECTED_SANDBOX.name,
            commissionRatePct = 10,
            isVerified = true,
            isOpenNow = true,
            tradeLicenseRef = "DSCC-TRAD-008814",
            lastSyncedAt = now - 420_000L
        ),
        MerchantEntity(
            id = "mer_mirpur_manual",
            name = "Mirpur Stitch Studio",
            storeType = StoreType.PHYSICAL_MANUAL.name,
            area = "Mirpur 10, Dhaka",
            address = "Plot 12, Begum Rokeya Sarani, Mirpur 10",
            distanceKm = 6.5,
            rating = 4.5,
            fulfilmentReliability = 0.89,
            avgPrepMinutes = 35,
            deliveryChargeBdt = 80,
            posProvider = null,
            posStatus = PosConnectionStatus.MANUAL_ONLY.name,
            commissionRatePct = 8,
            isVerified = true,
            isOpenNow = true,
            ownerUserId = "usr_nopos_merchant",
            tradeLicenseRef = "DNCC-TRAD-007721",
            lastSyncedAt = now - 2 * HOUR_MS
        ),
        MerchantEntity(
            id = "mer_dhaka_loom_online",
            name = "Dhaka Loom Atelier",
            storeType = StoreType.ONLINE_ONLY.name,
            area = "Gulshan 2, Dhaka",
            address = "Dispatch Studio 4B, Gulshan 2, Dhaka 1212",
            distanceKm = 2.6,
            rating = 4.8,
            fulfilmentReliability = 0.96,
            avgPrepMinutes = 30,
            deliveryChargeBdt = 65,
            posProvider = null,
            posStatus = PosConnectionStatus.MANUAL_ONLY.name,
            commissionRatePct = 12,
            isVerified = true,
            isOpenNow = true,
            ownerUserId = "usr_online_merchant",
            tradeLicenseRef = "DBID-BD-119204",
            lastSyncedAt = now - 900_000L
        ),
        MerchantEntity(
            id = "mer_uttara_street",
            name = "Uttara Street Collective",
            storeType = StoreType.PHYSICAL_POS.name,
            area = "Uttara Sector 7, Dhaka",
            address = "Lake Drive Road, Sector 7, Uttara",
            distanceKm = 9.2,
            rating = 4.6,
            fulfilmentReliability = 0.91,
            avgPrepMinutes = 25,
            deliveryChargeBdt = 90,
            posProvider = "RetailBD Cloud POS",
            posStatus = PosConnectionStatus.SYNC_DELAYED.name,
            commissionRatePct = 11,
            isVerified = false,
            isOpenNow = true,
            tradeLicenseRef = "DNCC-PENDING-9902",
            lastSyncedAt = now - 5 * HOUR_MS
        )
    )

    val demoProducts = listOf(
        ProductEntity(
            id = "prd_jamdani_panjabi",
            name = "Sonargaon Jamdani Cotton Panjabi",
            brand = "Deshi Loom",
            category = "Men",
            subCategory = "Panjabi & Heritage",
            fabric = "100% Combed Muslin Cotton (140 GSM)",
            description = "Tailored in Dhaka with subtle sage-ivory Jamdani motif weaving along the placket. Breathable weave engineered for humid Bangladesh afternoons and festive evenings.",
            careInstructions = "Gentle cold hand wash separately. Steam iron inside out.",
            returnPolicy = "7-day size exchange or return with intact hangtag.",
            imageKey = "panjabi",
            isNewArrival = true,
            isTrending = true,
            isBestSeller = true,
            rating = 4.9,
            reviewCount = 42
        ),
        ProductEntity(
            id = "prd_tangail_saree",
            name = "Tangail Ivory Muslin Saree",
            brand = "Taant Ghar",
            category = "Women",
            subCategory = "Sarees & Handloom",
            fabric = "Handloom Half-Silk Tangail Muslin",
            description = "Woven by master weavers in Tangail featuring an ivory body and a striking vermilion-coral geometric paar. Includes unstitched matching blouse piece.",
            careInstructions = "Dry clean recommended for first two washes.",
            returnPolicy = "5-day return on unused, unpinned items.",
            imageKey = "saree",
            isNewArrival = true,
            isTrending = true,
            isBestSeller = false,
            rating = 4.8,
            reviewCount = 29
        ),
        ProductEntity(
            id = "prd_forest_hoodie",
            name = "Dhaka Heavyweight Forest Hoodie",
            brand = "Padma Street",
            category = "Sportswear",
            subCategory = "Outerwear & Athleisure",
            fabric = "380 GSM Organic French Terry Cotton",
            description = "Relaxed drop-shoulder silhouette crafted from Narayanganj export-grade French terry with tonal embroidered crest.",
            careInstructions = "Machine wash cold inside out. Do not tumble dry.",
            returnPolicy = "7-day exchange across participating Dhaka stores.",
            imageKey = "streetwear",
            isNewArrival = false,
            isTrending = true,
            isBestSeller = true,
            rating = 4.7,
            reviewCount = 63
        ),
        ProductEntity(
            id = "prd_sage_overshirt",
            name = "Banani Sage Linen Overshirt",
            brand = "Deshi Loom",
            category = "Men",
            subCategory = "Casual Shirts",
            fabric = "Belgian-Blended Breathable Linen",
            description = "Structured collar, corozo nut buttons, and airy linen drape built for smart-casual Dhaka commutes.",
            careInstructions = "Cold wash, hang dry in shade.",
            returnPolicy = "7-day easy return or size swap.",
            imageKey = "hero",
            isNewArrival = true,
            isTrending = false,
            isBestSeller = true,
            rating = 4.8,
            reviewCount = 31
        ),
        ProductEntity(
            id = "prd_kids_kurta",
            name = "Festive Khadi Kids Kurta Set",
            brand = "Comilla Craft",
            category = "Kids",
            subCategory = "Ethnic Sets",
            fabric = "Handspun Comilla Khadi Cotton",
            description = "Ultra-soft pre-washed khadi kurta and elasticated pajama designed for comfort during Eid and Pohela Boishakh.",
            careInstructions = "Gentle machine wash.",
            returnPolicy = "7-day size exchange.",
            imageKey = "panjabi",
            isNewArrival = true,
            isTrending = false,
            isBestSeller = false,
            rating = 4.9,
            reviewCount = 19
        ),
        ProductEntity(
            id = "prd_women_kurti",
            name = "Coral Block-Print Muslin Kurti",
            brand = "Taant Ghar",
            category = "Women",
            subCategory = "Kurtis & Tunics",
            fabric = "Fine Voile & Muslin Blend",
            description = "Hand block-printed botanical motifs using eco-friendly terracotta dyes with side-seam pockets.",
            careInstructions = "Cold wash with mild detergent.",
            returnPolicy = "7-day return eligible.",
            imageKey = "saree",
            isNewArrival = false,
            isTrending = true,
            isBestSeller = true,
            rating = 4.6,
            reviewCount = 54
        )
    )

    val demoListings = listOf(
        // Panjabi offered by 3 merchants (shows real merchant comparison!)
        MerchantListingEntity(
            id = "lst_panjabi_banani_m",
            productId = "prd_jamdani_panjabi",
            merchantId = "mer_banani_pos",
            sku = "DL-PNJ-IVR-M",
            size = "M",
            color = "Ivory Sage",
            colorHex = 0xFFEAE3D2,
            priceBdt = 3250,
            discountPriceBdt = 2890,
            physicalQty = 8,
            reservedQty = 1,
            damagedQty = 0,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.LIVE_VERIFIED.name,
            lastUpdatedAt = now - 180_000L
        ),
        MerchantListingEntity(
            id = "lst_panjabi_banani_l",
            productId = "prd_jamdani_panjabi",
            merchantId = "mer_banani_pos",
            sku = "DL-PNJ-IVR-L",
            size = "L",
            color = "Ivory Sage",
            colorHex = 0xFFEAE3D2,
            priceBdt = 3250,
            discountPriceBdt = 2890,
            physicalQty = 3,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.LIVE_VERIFIED.name,
            lastUpdatedAt = now - 180_000L
        ),
        MerchantListingEntity(
            id = "lst_panjabi_dhanmondi_l",
            productId = "prd_jamdani_panjabi",
            merchantId = "mer_dhanmondi_pos",
            sku = "DL-PNJ-DHN-L",
            size = "L",
            color = "Ivory Sage",
            colorHex = 0xFFEAE3D2,
            priceBdt = 3100,
            discountPriceBdt = 2790,
            physicalQty = 5,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.LIVE_VERIFIED.name,
            lastUpdatedAt = now - 420_000L
        ),
        MerchantListingEntity(
            id = "lst_panjabi_mirpur_m",
            productId = "prd_jamdani_panjabi",
            merchantId = "mer_mirpur_manual",
            sku = "MR-PNJ-01-M",
            size = "M",
            color = "Ivory Sage",
            colorHex = 0xFFEAE3D2,
            priceBdt = 2950,
            discountPriceBdt = null,
            physicalQty = 4,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.MANUAL_UPDATE.name,
            stockConfidence = StockConfidence.MANUAL_RECENT.name,
            lastUpdatedAt = now - 2 * HOUR_MS
        ),
        // Tangail Saree
        MerchantListingEntity(
            id = "lst_saree_atelier",
            productId = "prd_tangail_saree",
            merchantId = "mer_dhaka_loom_online",
            sku = "TG-SAR-COR-OS",
            size = "Free Size",
            color = "Cream Coral",
            colorHex = 0xFFE94E35,
            priceBdt = 5400,
            discountPriceBdt = 4850,
            physicalQty = 11,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.MANUAL_UPDATE.name,
            stockConfidence = StockConfidence.MANUAL_RECENT.name,
            lastUpdatedAt = now - 900_000L
        ),
        MerchantListingEntity(
            id = "lst_saree_banani",
            productId = "prd_tangail_saree",
            merchantId = "mer_banani_pos",
            sku = "TG-SAR-BAN-OS",
            size = "Free Size",
            color = "Cream Coral",
            colorHex = 0xFFE94E35,
            priceBdt = 5500,
            discountPriceBdt = 4990,
            physicalQty = 2,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.LIVE_VERIFIED.name,
            lastUpdatedAt = now - 180_000L
        ),
        // Forest Hoodie
        MerchantListingEntity(
            id = "lst_hoodie_dhanmondi_l",
            productId = "prd_forest_hoodie",
            merchantId = "mer_dhanmondi_pos",
            sku = "PS-HD-FRS-L",
            size = "L",
            color = "Forest Charcoal",
            colorHex = 0xFF2E3A32,
            priceBdt = 2450,
            discountPriceBdt = 2150,
            physicalQty = 14,
            reservedQty = 0,
            damagedQty = 1,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.LIVE_VERIFIED.name,
            lastUpdatedAt = now - 420_000L
        ),
        MerchantListingEntity(
            id = "lst_hoodie_uttara_xl",
            productId = "prd_forest_hoodie",
            merchantId = "mer_uttara_street",
            sku = "PS-HD-UTT-XL",
            size = "XL",
            color = "Forest Charcoal",
            colorHex = 0xFF2E3A32,
            priceBdt = 2400,
            discountPriceBdt = null,
            physicalQty = 6,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.NEEDS_RECONCILIATION.name,
            lastUpdatedAt = now - 5 * HOUR_MS
        ),
        // Sage Overshirt
        MerchantListingEntity(
            id = "lst_overshirt_banani_m",
            productId = "prd_sage_overshirt",
            merchantId = "mer_banani_pos",
            sku = "DL-OVS-SAG-M",
            size = "M",
            color = "Heritage Sage",
            colorHex = 0xFFA7B69B,
            priceBdt = 2650,
            discountPriceBdt = null,
            physicalQty = 7,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.POS_SYNC_SANDBOX.name,
            stockConfidence = StockConfidence.LIVE_VERIFIED.name,
            lastUpdatedAt = now - 180_000L
        ),
        MerchantListingEntity(
            id = "lst_overshirt_online_l",
            productId = "prd_sage_overshirt",
            merchantId = "mer_dhaka_loom_online",
            sku = "DL-OVS-SAG-L",
            size = "L",
            color = "Heritage Sage",
            colorHex = 0xFFA7B69B,
            priceBdt = 2650,
            discountPriceBdt = 2390,
            physicalQty = 9,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.MANUAL_UPDATE.name,
            stockConfidence = StockConfidence.MANUAL_RECENT.name,
            lastUpdatedAt = now - 900_000L
        ),
        // Kids Kurta
        MerchantListingEntity(
            id = "lst_kids_mirpur_s",
            productId = "prd_kids_kurta",
            merchantId = "mer_mirpur_manual",
            sku = "CC-KID-KHD-6Y",
            size = "6-8Y",
            color = "Natural Khadi",
            colorHex = 0xFFD9CFB8,
            priceBdt = 1450,
            discountPriceBdt = 1290,
            physicalQty = 10,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.MANUAL_UPDATE.name,
            stockConfidence = StockConfidence.MANUAL_RECENT.name,
            lastUpdatedAt = now - 2 * HOUR_MS
        ),
        // Women Kurti
        MerchantListingEntity(
            id = "lst_kurti_online_m",
            productId = "prd_women_kurti",
            merchantId = "mer_dhaka_loom_online",
            sku = "TG-KRT-COR-M",
            size = "M",
            color = "Terracotta Coral",
            colorHex = 0xFFE94E35,
            priceBdt = 1950,
            discountPriceBdt = 1690,
            physicalQty = 12,
            reservedQty = 0,
            damagedQty = 0,
            inventorySource = InventorySource.MANUAL_UPDATE.name,
            stockConfidence = StockConfidence.MANUAL_RECENT.name,
            lastUpdatedAt = now - 900_000L
        )
    )

    val demoDeliveryZones = listOf(
        DeliveryZoneEntity(
            id = "zone_dhaka_express",
            name = "Dhaka Metro Express",
            areas = "Banani, Gulshan, Baridhara, Mohakhali, Dhanmondi, Tejgaon",
            baseChargeBdt = 60,
            expressAvailable = true,
            maxDistanceKm = 12.0,
            estimatedWindow = "Same-Day (2–4 hrs)",
            isActive = true
        ),
        DeliveryZoneEntity(
            id = "zone_dhaka_outer",
            name = "Dhaka Outer Standard",
            areas = "Mirpur, Uttara, Mohammadpur, Motijheel, Jatrabari",
            baseChargeBdt = 85,
            expressAvailable = false,
            maxDistanceKm = 25.0,
            estimatedWindow = "Next-Day (18–24 hrs)",
            isActive = true
        ),
        DeliveryZoneEntity(
            id = "zone_nationwide",
            name = "Chattogram & Nationwide Courier",
            areas = "Chattogram, Sylhet, Rajshahi, Khulna",
            baseChargeBdt = 130,
            expressAvailable = false,
            maxDistanceKm = 320.0,
            estimatedWindow = "2–3 Business Days",
            isActive = true
        )
    )

    val demoAddresses = listOf(
        SavedAddressEntity(
            id = "addr_1",
            customerId = "usr_customer_1",
            label = "Home · Banani",
            addressLine = "House 24, Road 11, Block F, Banani, Dhaka 1213",
            zoneName = "Dhaka Metro Express",
            phone = "+880 1711-440219",
            isDefault = true
        ),
        SavedAddressEntity(
            id = "addr_2",
            customerId = "usr_customer_1",
            label = "Studio · Dhanmondi",
            addressLine = "Level 4, House 18, Road 27, Dhanmondi, Dhaka 1209",
            zoneName = "Dhaka Metro Express",
            phone = "+880 1711-440219",
            isDefault = false
        )
    )

    val demoReviews = listOf(
        ReviewEntity(
            id = "rev_1",
            productId = "prd_jamdani_panjabi",
            merchantId = "mer_banani_pos",
            customerName = "Tanvir Mahmud",
            rating = 5,
            comment = "Muslin breathability is unmatched in Dhaka humidity. Same-day Banani pickup arrived in 95 minutes.",
            verifiedPurchase = true,
            createdAt = now - 48 * HOUR_MS
        ),
        ReviewEntity(
            id = "rev_2",
            productId = "prd_tangail_saree",
            merchantId = "mer_dhaka_loom_online",
            customerName = "Mehnaz Chowdhury",
            rating = 5,
            comment = "True Tangail weave and the coral border matches the Porey lookbook photos accurately.",
            verifiedPurchase = true,
            createdAt = now - 30 * HOUR_MS
        ),
        ReviewEntity(
            id = "rev_3",
            productId = "prd_forest_hoodie",
            merchantId = "mer_dhanmondi_pos",
            customerName = "Rafiqul Islam",
            rating = 5,
            comment = "Heavy 380 GSM French terry holds structure nicely. Stock count matched what Dhanmondi store had.",
            verifiedPurchase = true,
            createdAt = now - 16 * HOUR_MS
        )
    )

    val demoMovements = listOf(
        StockMovementEntity(
            id = "mov_seed_1",
            listingId = "lst_panjabi_banani_m",
            merchantId = "mer_banani_pos",
            sku = "DL-PNJ-IVR-M",
            deltaPhysical = 8,
            deltaReserved = 0,
            deltaDamaged = 0,
            reason = StockMovementReason.RESTOCK.name,
            actorRole = UserRole.PHYSICAL_MERCHANT.name,
            note = "Initial RetailBD POS sync batch (Seed)",
            createdAt = now - 4 * HOUR_MS
        ),
        StockMovementEntity(
            id = "mov_seed_2",
            listingId = "lst_panjabi_mirpur_m",
            merchantId = "mer_mirpur_manual",
            sku = "MR-PNJ-01-M",
            deltaPhysical = 4,
            deltaReserved = 0,
            deltaDamaged = 0,
            reason = StockMovementReason.EOD_RECONCILE.name,
            actorRole = UserRole.NO_POS_MERCHANT.name,
            note = "Morning shelf count verified manually",
            createdAt = now - 2 * HOUR_MS
        )
    )

    val demoNotifications = listOf(
        NotificationEntity(
            id = "not_1",
            targetRole = UserRole.CUSTOMER.name,
            title = "Pohela & Heritage Drop Live",
            message = "Use promo POREY10 for 10% off or DHAKA50 for ৳50 off across verified Dhaka shops.",
            category = "PROMO",
            createdAt = now - 3 * HOUR_MS
        ),
        NotificationEntity(
            id = "not_2",
            targetRole = UserRole.ADMIN.name,
            title = "POS Sync Delay Alert · Uttara",
            message = "Uttara Street Collective POS webhook heartbeat delayed > 4 hours. Stock marked 'Verify Availability'.",
            category = "INTEGRATION_ALERT",
            createdAt = now - HOUR_MS
        )
    )

    val demoAuditLogs = listOf(
        AuditLogEntity(
            id = "aud_1",
            actorRole = UserRole.SUPER_ADMIN.name,
            actorName = "System Bootstrap",
            action = "SANDBOX_SEED_INIT",
            targetEntity = "Ecosystem",
            details = "Seeded Bangladesh demo merchants, delivery zones, and sandbox POS/MFS adapters.",
            createdAt = now - 6 * HOUR_MS
        )
    )
}
