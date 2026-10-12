package com.example.alertascripto

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Checker {
    // ids do CoinGecko: muda esta lista para vigiar outras moedas
    val MOEDAS = listOf("bitcoin", "ethereum", "solana")
    const val FIAT = "eur"
    const val RSI_COMPRA = 30.0
    const val RSI_VENDA = 70.0

    data class Resultado(val moeda: String, val preco: Double, val rsi: Double, val sinal: String, val motivo: String) {
        fun linha() = "${moeda.uppercase()}: %.2f ${FIAT.uppercase()} | RSI %.0f | $sinal ($motivo)".format(preco, rsi)
    }

    fun precos(moeda: String): List<Double> {
        val url = URL("https://api.coingecko.com/api/v3/coins/$moeda/market_chart?vs_currency=$FIAT&days=30")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 20000
        c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "AlertasCripto/1.0")
        try {
            val arr = JSONObject(c.inputStream.bufferedReader().readText()).getJSONArray("prices")
            return List(arr.length()) { arr.getJSONArray(it).getDouble(1) }
        } finally {
            c.disconnect()
        }
    }

    private fun sma(v: List<Double>, n: Int) = v.takeLast(n).average()

    private fun rsi(v: List<Double>, n: Int = 14): Double {
        val t = v.takeLast(n + 1)
        var ganhos = 0.0
        var perdas = 0.0
        for (i in 1..n) {
            val d = t[i] - t[i - 1]
            if (d > 0) ganhos += d else perdas -= d
        }
        if (perdas == 0.0) return 100.0
        return 100 - 100 / (1 + ganhos / perdas)
    }

    fun avaliar(moeda: String): Resultado {
        val v = precos(moeda)
        val r = rsi(v)
        val alta = sma(v, 20) > sma(v, 50)
        return when {
            r <= RSI_COMPRA -> Resultado(moeda, v.last(), r, "COMPRAR", "RSI em sobrevenda" + if (alta) " e tendência de alta" else "")
            r >= RSI_VENDA -> Resultado(moeda, v.last(), r, "VENDER", "RSI em sobrecompra" + if (alta) "" else " e tendência de baixa")
            else -> Resultado(moeda, v.last(), r, "ESPERAR", "sem sinal claro")
        }
    }
}
