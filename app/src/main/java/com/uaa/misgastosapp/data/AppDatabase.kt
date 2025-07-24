// AppDatabase

package com.uaa.misgastosapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

// se crea una clase para convertir tipos de datos que room no entiende por si solo.
class Converters {
    // convierte un tipo de dato 'recurrencetype' a texto (string) para poder guardarlo.
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    // convierte el texto guardado de vuelta al tipo de dato 'recurrencetype'.
    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)
}

// aca se define la base de datos con todas sus propiedades.
@Database(
    // se le indica cuales son todas las tablas (entidades) que tendra.
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        UserEntity::class
    ],
    // se especifica la version de la base de datos, importante para futuras actualizaciones.
    version = 4
)
// se le dice a room que use la clase 'converters' para los tipos de datos especiales.
@TypeConverters(Converters::class)
// esta es la clase principal de la base de datos. es abstracta porque room se encarga de implementarla.
abstract class AppDatabase : RoomDatabase() {
    // se definen funciones abstractas para acceder a cada una de las tablas (a traves de los dao).
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun userDao(): UserDao

    // se crea un 'companion object' para poder tener una sola instancia de la base de datos en toda la app.
    companion object {
        // aca se guardara la unica instancia de la base de datos. 'volatile' se usa para que sea seguro entre diferentes hilos.
        @Volatile private var INSTANCE: AppDatabase? = null

        // esta es la funcion publica que se usara para obtener la instancia de la base de datos.
        fun getInstance(context: Context): AppDatabase {
            // se devuelve la instancia si ya existe. si no, se crea de forma segura.
            return INSTANCE ?: synchronized(this) {
                // aca dentro, se vuelve a revisar por si otro hilo la creo mientras se esperaba.
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gastos_db" // este es el nombre del archivo de la base de datos.
                )
                    // si se actualiza la 'version', esto destruira la base de datos anterior y creara una nueva. se pierden los datos.
                    .fallbackToDestructiveMigration()
                    // se construye la instancia de la base de datos.
                    .build().also { INSTANCE = it } // una vez creada, se guarda en la variable 'instance' para usos futuros.
            }
        }
    }
}