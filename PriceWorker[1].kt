package com.example.alertascripto

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class PriceWorker(private val ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val prefs = ctx.getSharedPreferences("cripto", Context.MODE_PRIVATE)
        val linhas = mutableListOf<String>()
        for ((i, moeda) in Checker.MOEDAS.withIndex()) {
            try {
                val r = Checker.avaliar(moeda)
                linhas.add(r.linha())
                val anterior = prefs.getString("sinal_$moeda", "")
                if (r.sinal != "ESPERAR" && r.sinal != anterior) notificar(i, r)
                prefs.edit().putString("sinal_$moeda", r.sinal).apply()
            } catch (e: Exception) {
                linhas.add("${moeda.uppercase()}: erro ao obter preços")
            }
            Thread.sleep(2000) // respeita o limite da API gratuita
        }
        val hora = java.text.SimpleDateFormat("HH:mm").format(java.util.Date())
        prefs.edit().putString("ultimo", "Atualizado às $hora\n\n" + linhas.joinToString("\n\n")).apply()
        return Result.success()
    }

    private fun notificar(id: Int, r: Checker.Resultado) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("avisos", "Avisos de cripto", NotificationManager.IMPORTANCE_HIGH))
        val n = NotificationCompat.Builder(ctx, "avisos")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${r.sinal}: ${r.moeda.uppercase()}")
            .setContentText(r.linha())
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.linha()))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
