package com.ror.nms2go.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SenderEntity::class, SentOrderEntity::class, OrderItemEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun senderDao(): SenderDao

    abstract fun orderDao(): OrderDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE senders ADD COLUMN company_name TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE senders ADD COLUMN receiver_email TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE senders ADD COLUMN parser TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `orders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sent_at` INTEGER NOT NULL,
                        `company` TEXT NOT NULL,
                        `sender_email` TEXT NOT NULL,
                        `receiver_email` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `error` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_orders_sent_at` ON `orders` (`sent_at`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `order_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `order_id` INTEGER NOT NULL,
                        `code` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `price` REAL,
                        `quantity` INTEGER NOT NULL,
                        FOREIGN KEY(`order_id`) REFERENCES `orders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_order_items_order_id` ON `order_items` (`order_id`)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE senders ADD COLUMN skip_keywords TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sender email becomes the primary key, so the table is rebuilt.
                // Rows duplicated by email (case-insensitive) are collapsed, keeping
                // the earliest row – duplicates crash the overview list keyed by email.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `senders_new` (
                        `email` TEXT NOT NULL,
                        `company_name` TEXT NOT NULL,
                        `receiver_email` TEXT NOT NULL DEFAULT '',
                        `parser` TEXT NOT NULL DEFAULT '',
                        `skip_keywords` TEXT NOT NULL DEFAULT '',
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`email`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO senders_new
                        (email, company_name, receiver_email, parser, skip_keywords, created_at)
                    SELECT TRIM(email), company_name, receiver_email, parser, skip_keywords, created_at
                    FROM senders
                    WHERE id IN (SELECT MAX(id) FROM senders GROUP BY LOWER(TRIM(email)))
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE senders")
                db.execSQL("ALTER TABLE senders_new RENAME TO senders")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nms2.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                ).build()
                    .also { instance = it }
            }
        }
    }
}
