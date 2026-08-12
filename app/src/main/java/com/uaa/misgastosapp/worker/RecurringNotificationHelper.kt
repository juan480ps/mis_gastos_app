// RecurringNotificationHelper

package com.uaa.misgastosapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.uaa.misgastosapp.R
import com.uaa.misgastosapp.data.repository.ProcessedRecurringTransaction
import java.text.NumberFormat
import java.util.Locale

/**
 * Notifica al usuario cuando se procesan transacciones recurrentes (se registran automaticamente
 * como una transaccion real). Antes esto pasaba en silencio: el balance cambiaba sin ninguna
 * señal visible, asi que la fecha de "proximo vencimiento" no tenia ninguna consecuencia visible
 * para el usuario.
 */
object RecurringNotificationHelper {
    private const val CHANNEL_ID = "recurring_transactions"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transacciones recurrentes",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisa cuando se registra automáticamente un gasto o ingreso recurrente."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun notifyProcessed(context: Context, processed: List<ProcessedRecurringTransaction>) {
        if (processed.isEmpty()) return

        // en Android 13+ hace falta el permiso POST_NOTIFICATIONS concedido en tiempo de
        // ejecucion; si el usuario no lo dio, no se puede postear (evita un crash de SecurityException).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "PY")).apply { maximumFractionDigits = 0 }
        val title = if (processed.size == 1) {
            "Transacción recurrente registrada"
        } else {
            "${processed.size} transacciones recurrentes registradas"
        }
        val detail = processed.joinToString(separator = "\n") { item ->
            "${item.title}: ${currencyFormat.format(item.amount)}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(detail.lines().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
