package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.PoreyDao
import com.example.data.local.entity.AuditLogEntity
import com.example.data.local.entity.CartItemEntity
import com.example.data.local.entity.DeliveryZoneEntity
import com.example.data.local.entity.FinancialLedgerEntity
import com.example.data.local.entity.FulfilmentOrderEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.MerchantListingEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.OrderEntity
import com.example.data.local.entity.OrderItemEntity
import com.example.data.local.entity.PosWebhookEventEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ReturnRequestEntity
import com.example.data.local.entity.ReviewEntity
import com.example.data.local.entity.SavedAddressEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.UserAccountEntity
import com.example.data.local.entity.WishlistItemEntity

@Database(
    entities = [
        UserAccountEntity::class,
        MerchantEntity::class,
        ProductEntity::class,
        MerchantListingEntity::class,
        StockMovementEntity::class,
        PosWebhookEventEntity::class,
        CartItemEntity::class,
        OrderEntity::class,
        FulfilmentOrderEntity::class,
        OrderItemEntity::class,
        FinancialLedgerEntity::class,
        ReturnRequestEntity::class,
        ReviewEntity::class,
        DeliveryZoneEntity::class,
        NotificationEntity::class,
        AuditLogEntity::class,
        WishlistItemEntity::class,
        SavedAddressEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PoreyDatabase : RoomDatabase() {
    abstract fun poreyDao(): PoreyDao

    companion object {
        @Volatile
        private var INSTANCE: PoreyDatabase? = null

        fun getInstance(context: Context): PoreyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PoreyDatabase::class.java,
                    "porey_commerce_bd.db"
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
