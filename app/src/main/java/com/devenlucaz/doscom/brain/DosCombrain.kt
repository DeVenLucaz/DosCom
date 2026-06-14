package com.devenlucaz.doscom.brain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Random

class LIFCore(val nIn: Int, val nHid: Int) {
    val membrane = FloatArray(nHid) { -65f }
    val vRest = -65f
    val vThresh = -50f
    val tau = 10f
    
    private val random = Random()
    private fun gaussian() = random.nextGaussian().toFloat()
    
    val weightsIn = Array(nIn) { FloatArray(nHid) { gaussian() * 8f } }
    val recurrent = Array(nHid) { FloatArray(nHid) { gaussian() * 0.5f } }

    fun update(input: FloatArray, lastSpikes: FloatArray): FloatArray {
        val spikes = FloatArray(nHid)
        for (j in 0 until nHid) {
            var current = 0f
            for (i in 0 until nIn) {
                current += input[i] * weightsIn[i][j]
            }
            for (k in 0 until nHid) {
                current += lastSpikes[k] * recurrent[k][j]
            }
            val dv = (vRest - membrane[j] + current) / tau
            membrane[j] += dv
            if (membrane[j] >= vThresh) {
                spikes[j] = 1f
                membrane[j] = vRest
            } else {
                spikes[j] = 0f
            }
        }
        return spikes
    }
}

class DosCombrain {
    val core = LIFCore(8, 48)
    private val random = Random()
    private fun gaussian() = random.nextGaussian().toFloat()
    
    val decisionLayer = Array(48) { FloatArray(19) { gaussian() * 0.1f } }
    var lastSpikes = FloatArray(48)
    var lastConfidence = 0f

    fun think(inputs: FloatArray, duration: Int = 150): IntArray {
        val spikeCounts = FloatArray(48)
        var spikes = lastSpikes
        for (t in 0 until duration) {
            spikes = core.update(inputs, spikes)
            for (i in 0 until 48) {
                spikeCounts[i] += spikes[i]
            }
        }
        lastSpikes = spikes

        var maxOutputScore = 0f
        val outputScores = FloatArray(19)
        for (i in 0 until 19) {
            var score = 0f
            for (h in 0 until 48) {
                score += spikeCounts[h] * decisionLayer[h][i]
            }
            outputScores[i] = score
            if (score > maxOutputScore) maxOutputScore = score
        }
        lastConfidence = maxOutputScore

        val decisions = IntArray(7)
        
        var bestMime = 0
        var maxMime = -Float.MAX_VALUE
        for (i in 0..5) {
            if (outputScores[i] > maxMime) { maxMime = outputScores[i]; bestMime = i }
        }
        decisions[0] = bestMime

        var bestIdle = 6
        var maxIdle = -Float.MAX_VALUE
        for (i in 6..10) {
            if (outputScores[i] > maxIdle) { maxIdle = outputScores[i]; bestIdle = i }
        }
        decisions[1] = bestIdle

        var bestToy = 11
        var maxToy = -Float.MAX_VALUE
        for (i in 11..14) {
            if (outputScores[i] > maxToy) { maxToy = outputScores[i]; bestToy = i }
        }
        decisions[2] = bestToy

        decisions[3] = if (outputScores[15] > 0) 1 else 0
        decisions[4] = if (outputScores[16] > 0) 1 else 0
        decisions[5] = if (outputScores[17] > 0) 1 else 0
        decisions[6] = if (outputScores[18] > 0) 1 else 0

        return decisions
    }

    fun learn(inputs: FloatArray, targetOutputs: IntArray, reward: Float, epochs: Int = 20) {
        val spikeCounts = FloatArray(48)
        var spikes = lastSpikes
        for (t in 0 until 150) {
            spikes = core.update(inputs, spikes)
            for (i in 0 until 48) {
                spikeCounts[i] += spikes[i]
            }
        }
        
        val lr = 0.01f * reward
        for (epoch in 0 until epochs) {
            for (h in 0 until 48) {
                if (spikeCounts[h] > 0) {
                    for (i in 0 until 8) {
                        if (inputs[i] > 0.5f) {
                            core.weightsIn[i][h] += lr
                        }
                    }
                    for (o in 0 until 19) {
                        val wasSelected = (o == targetOutputs[0] || o == targetOutputs[1] || o == targetOutputs[2] || 
                                          (o == 15 && targetOutputs[3] == 1) ||
                                          (o == 16 && targetOutputs[4] == 1) ||
                                          (o == 17 && targetOutputs[5] == 1) ||
                                          (o == 18 && targetOutputs[6] == 1))
                        
                        if (wasSelected) {
                            decisionLayer[h][o] += lr
                        } else {
                            decisionLayer[h][o] -= lr * 0.1f 
                        }
                    }
                }
            }
        }
    }

    fun save(context: Context) {
        try {
            val json = JSONObject()
            val wInArray = JSONArray()
            for (i in 0 until 8) {
                val row = JSONArray()
                for (j in 0 until 48) { row.put(core.weightsIn[i][j].toDouble()) }
                wInArray.put(row)
            }
            json.put("weightsIn", wInArray)

            val recArray = JSONArray()
            for (i in 0 until 48) {
                val row = JSONArray()
                for (j in 0 until 48) { row.put(core.recurrent[i][j].toDouble()) }
                recArray.put(row)
            }
            json.put("recurrent", recArray)

            val decArray = JSONArray()
            for (i in 0 until 48) {
                val row = JSONArray()
                for (j in 0 until 19) { row.put(decisionLayer[i][j].toDouble()) }
                decArray.put(row)
            }
            json.put("decisionLayer", decArray)

            val file = File(context.filesDir, "brain.json")
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun load(context: Context): Boolean {
        val file = File(context.filesDir, "brain.json")
        if (!file.exists()) return false
        return try {
            val json = JSONObject(file.readText())
            val wInArray = json.getJSONArray("weightsIn")
            for (i in 0 until 8) {
                val row = wInArray.getJSONArray(i)
                for (j in 0 until 48) { core.weightsIn[i][j] = row.getDouble(j).toFloat() }
            }

            val recArray = json.getJSONArray("recurrent")
            for (i in 0 until 48) {
                val row = recArray.getJSONArray(i)
                for (j in 0 until 48) { core.recurrent[i][j] = row.getDouble(j).toFloat() }
            }

            val decArray = json.getJSONArray("decisionLayer")
            for (i in 0 until 48) {
                val row = decArray.getJSONArray(i)
                for (j in 0 until 19) { decisionLayer[i][j] = row.getDouble(j).toFloat() }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun reset(context: Context) {
        val file = File(context.filesDir, "brain.json")
        if (file.exists()) {
            file.delete()
        }
        for (i in 0 until 8) {
            for (j in 0 until 48) { core.weightsIn[i][j] = gaussian() * 8f }
        }
        for (i in 0 until 48) {
            for (j in 0 until 48) { core.recurrent[i][j] = gaussian() * 0.5f }
            for (j in 0 until 19) { decisionLayer[i][j] = gaussian() * 0.1f }
        }
    }
}
