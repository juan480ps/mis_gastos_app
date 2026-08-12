package com.uaa.misgastosapp.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database migrations.
 * Each migration defines SQL statements to upgrade from one version to the next.
 * This preserves user data across app updates instead of destroying and recreating tables.
 */
object Migrations {

    /**
     * Migration from version 1 to 2:
     * Added recurring_transactions table.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS recurring_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    amount REAL NOT NULL,
                    categoryId INTEGER,
                    recurrenceType TEXT NOT NULL,
                    dayOfMonth INTEGER NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT,
                    nextDueDate TEXT NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE SET_NULL
                )
            """)
        }
    }

    /**
     * Migration from version 2 to 3:
     * Added users table.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    email TEXT NOT NULL,
                    password TEXT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt TEXT NOT NULL
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")
        }
    }

    /**
     * Migration from version 3 to 4:
     * Added indices on categoryId for TransactionEntity and RecurringTransactionEntity.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_transactions_categoryId ON recurring_transactions(categoryId)")
        }
    }

    /**
     * Migration from version 4 to 5:
     * Placeholder for future schema changes.
     * Add new migrations here as the schema evolves.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Future migrations go here
        }
    }

    /**
     * Migration from version 5 to 6:
     * Added accounts table (cuentas/bancos opcionales) y la columna accountId en transactions,
     * para poder separar los gastos por banco sin que sea obligatorio (accountId es nullable).
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    bankName TEXT,
                    colorHex TEXT NOT NULL
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_name ON accounts(name)")
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN accountId INTEGER REFERENCES accounts(id) ON DELETE SET NULL"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        }
    }

    /**
     * Migration from version 6 to 7:
     * Añade la columna isRecurring en budgets, para presupuestos que aplican a todos los meses
     * en vez de tener que configurarse mes a mes.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budgets ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * List of all migrations in order.
     * Add new migrations to this list when upgrading the database version.
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7
    )
}
