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
    
    val weightsIn = Array(nIn) { FloatArray(nHid) { gaussian() * 4f } }
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
    // Topologies
    val inputSize = 16
    val subSize = 24
    val leftSize = 48
    val rightSize = 48
    val consciousSize = 64
    val outputSize = 15 // 0-5 (Idle), 6-10 (Work), 11-14 (Magic)

    val subCore = LIFCore(inputSize, subSize)
    val leftCore = LIFCore(inputSize, leftSize)
    val rightCore = LIFCore(inputSize, rightSize)
    val consciousCore = LIFCore(subSize + leftSize + rightSize, consciousSize)
    
    private val random = Random()
    private fun gaussian() = random.nextGaussian().toFloat()
    
    val decisionLayer = Array(consciousSize) { FloatArray(outputSize) { gaussian() * 0.1f } }
    
    var lastSubSpikes = FloatArray(subSize)
    var lastLeftSpikes = FloatArray(leftSize)
    var lastRightSpikes = FloatArray(rightSize)
    var lastConsciousSpikes = FloatArray(consciousSize)
    
    var lastConfidence = 0f

    fun think(inputs: FloatArray, duration: Int = 150): IntArray {
        val spikeCounts = FloatArray(consciousSize)
        
        var subSpikes = lastSubSpikes
        var leftSpikes = lastLeftSpikes
        var rightSpikes = lastRightSpikes
        var conSpikes = lastConsciousSpikes
        
        for (t in 0 until duration) {
            subSpikes = subCore.update(inputs, subSpikes)
            leftSpikes = leftCore.update(inputs, leftSpikes)
            rightSpikes = rightCore.update(inputs, rightSpikes)
            
            // Combine inputs for conscious core
            val conInputs = FloatArray(subSize + leftSize + rightSize)
            System.arraycopy(subSpikes, 0, conInputs, 0, subSize)
            System.arraycopy(leftSpikes, 0, conInputs, subSize, leftSize)
            System.arraycopy(rightSpikes, 0, conInputs, subSize + leftSize, rightSize)
            
            conSpikes = consciousCore.update(conInputs, conSpikes)
            
            for (i in 0 until consciousSize) {
                spikeCounts[i] += conSpikes[i]
            }
        }
        
        lastSubSpikes = subSpikes
        lastLeftSpikes = leftSpikes
        lastRightSpikes = rightSpikes
        lastConsciousSpikes = conSpikes

        var maxOutputScore = 0f
        val outputScores = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            var score = 0f
            for (h in 0 until consciousSize) {
                score += spikeCounts[h] * decisionLayer[h][i]
            }
            outputScores[i] = score
            if (score > maxOutputScore) maxOutputScore = score
        }
        lastConfidence = maxOutputScore

        val decisions = IntArray(3)
        
        // 0: bestIdle (0..5)
        var bestIdle = 0
        var maxIdle = -Float.MAX_VALUE
        for (i in 0..5) {
            if (outputScores[i] > maxIdle) { maxIdle = outputScores[i]; bestIdle = i }
        }
        decisions[0] = bestIdle

        // 1: bestWork (6..10)
        var bestWork = 6
        var maxWork = -Float.MAX_VALUE
        for (i in 6..10) {
            if (outputScores[i] > maxWork) { maxWork = outputScores[i]; bestWork = i }
        }
        decisions[1] = bestWork

        // 2: bestMagic (11..14)
        var bestMagic = 11
        var maxMagic = -Float.MAX_VALUE
        for (i in 11..14) {
            if (outputScores[i] > maxMagic) { maxMagic = outputScores[i]; bestMagic = i }
        }
        decisions[2] = bestMagic

        return decisions
    }

    fun learn(inputs: FloatArray, targetOutputs: IntArray, reward: Float, epochs: Int = 20) {
        val spikeCounts = FloatArray(consciousSize)
        var subSpikes = lastSubSpikes
        var leftSpikes = lastLeftSpikes
        var rightSpikes = lastRightSpikes
        var conSpikes = lastConsciousSpikes
        
        for (t in 0 until 150) {
            subSpikes = subCore.update(inputs, subSpikes)
            leftSpikes = leftCore.update(inputs, leftSpikes)
            rightSpikes = rightCore.update(inputs, rightSpikes)
            
            val conInputs = FloatArray(subSize + leftSize + rightSize)
            System.arraycopy(subSpikes, 0, conInputs, 0, subSize)
            System.arraycopy(leftSpikes, 0, conInputs, subSize, leftSize)
            System.arraycopy(rightSpikes, 0, conInputs, subSize + leftSize, rightSize)
            
            conSpikes = consciousCore.update(conInputs, conSpikes)
            
            for (i in 0 until consciousSize) {
                spikeCounts[i] += conSpikes[i]
            }
        }
        
        val lr = 0.01f * reward
        for (epoch in 0 until epochs) {
            for (h in 0 until consciousSize) {
                if (spikeCounts[h] > 0) {
                    for (o in 0 until outputSize) {
                        val wasSelected = (o == targetOutputs[0] || o == targetOutputs[1] || o == targetOutputs[2])
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

    private fun saveWeights(json: JSONObject, key: String, weights: Array<FloatArray>) {
        val arr = JSONArray()
        for (row in weights) {
            val r = JSONArray()
            for (v in row) r.put(v.toDouble())
            arr.put(r)
        }
        json.put(key, arr)
    }

    private fun loadWeights(json: JSONObject, key: String, target: Array<FloatArray>) {
        if (!json.has(key)) return
        val arr = json.getJSONArray(key)
        for (i in 0 until minOf(arr.length(), target.size)) {
            val r = arr.getJSONArray(i)
            for (j in 0 until minOf(r.length(), target[i].size)) {
                target[i][j] = r.getDouble(j).toFloat()
            }
        }
    }

    fun save(context: Context) {
        try {
            val json = JSONObject()
            json.put("version", 3)

            // Save all 4 cores
            saveWeights(json, "sub_wIn", subCore.weightsIn)
            saveWeights(json, "sub_rec", subCore.recurrent)
            saveWeights(json, "left_wIn", leftCore.weightsIn)
            saveWeights(json, "left_rec", leftCore.recurrent)
            saveWeights(json, "right_wIn", rightCore.weightsIn)
            saveWeights(json, "right_rec", rightCore.recurrent)
            saveWeights(json, "con_wIn", consciousCore.weightsIn)
            saveWeights(json, "con_rec", consciousCore.recurrent)

            // Save decision layer
            saveWeights(json, "decisionLayer", decisionLayer)

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
            if (!json.has("version") || json.getInt("version") != 3) return false

            loadWeights(json, "sub_wIn", subCore.weightsIn)
            loadWeights(json, "sub_rec", subCore.recurrent)
            loadWeights(json, "left_wIn", leftCore.weightsIn)
            loadWeights(json, "left_rec", leftCore.recurrent)
            loadWeights(json, "right_wIn", rightCore.weightsIn)
            loadWeights(json, "right_rec", rightCore.recurrent)
            loadWeights(json, "con_wIn", consciousCore.weightsIn)
            loadWeights(json, "con_rec", consciousCore.recurrent)

            loadWeights(json, "decisionLayer", decisionLayer)
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
        // Re-randomize all cores
        for (i in subCore.weightsIn.indices) for (j in subCore.weightsIn[i].indices) subCore.weightsIn[i][j] = gaussian() * 4f
        for (i in subCore.recurrent.indices) for (j in subCore.recurrent[i].indices) subCore.recurrent[i][j] = gaussian() * 0.5f
        for (i in leftCore.weightsIn.indices) for (j in leftCore.weightsIn[i].indices) leftCore.weightsIn[i][j] = gaussian() * 4f
        for (i in leftCore.recurrent.indices) for (j in leftCore.recurrent[i].indices) leftCore.recurrent[i][j] = gaussian() * 0.5f
        for (i in rightCore.weightsIn.indices) for (j in rightCore.weightsIn[i].indices) rightCore.weightsIn[i][j] = gaussian() * 4f
        for (i in rightCore.recurrent.indices) for (j in rightCore.recurrent[i].indices) rightCore.recurrent[i][j] = gaussian() * 0.5f
        for (i in consciousCore.weightsIn.indices) for (j in consciousCore.weightsIn[i].indices) consciousCore.weightsIn[i][j] = gaussian() * 4f
        for (i in consciousCore.recurrent.indices) for (j in consciousCore.recurrent[i].indices) consciousCore.recurrent[i][j] = gaussian() * 0.5f
        for (i in 0 until consciousSize) {
            for (j in 0 until outputSize) { decisionLayer[i][j] = gaussian() * 0.1f }
        }
    }
}
