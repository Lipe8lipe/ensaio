package com.example.alertascripto

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var estado: TextView

    private val pedirPermissao = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        val ativar = Button(this).apply { text = "Ativar avisos (a cada 15 min)" }
        val agora = Button(this).apply { text = "Verificar agora" }
        estado = TextView(this).apply { textSize = 16f; setPadding(0, pad, 0, 0) }
        col.addView(ativar, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        col.addView(agora, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        col.addView(estado)
        setContentView(ScrollView(this).apply { addView(col) })

        ativar.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
            val pedido = PeriodicWorkRequestBuilder<PriceWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork("avisos", ExistingPeriodicWorkPolicy.UPDATE, pedido)
            estado.text = "Avisos ativos. Podes fechar a app."
        }
        agora.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) pedirPermissao.launch(Manifest.permission.POST_NOTIFICATIONS)
            WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<PriceWorker>().build())
            estado.text = "A verificar… volta a abrir a app dentro de alguns segundos."
        }
    }

    override fun onResume() {
        super.onResume()
        val ultimo = getSharedPreferences("cripto", Context.MODE_PRIVATE).getString("ultimo", null)
        if (ultimo != null) estado.text = ultimo
    }
}
