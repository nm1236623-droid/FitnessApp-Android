package com.example.fitness.coach

import android.util.Base64
import com.example.fitness.data.ExerciseEntry
import org.json.JSONArray
import org.json.JSONObject

object ShareCodec {
    private const val PREFIX = "FITNESS_SHARE_V1:"

    sealed class DecodeResult {
        data class Success(val share: CoachPlanShare) : DecodeResult()
        data class Error(val message: String) : DecodeResult()
    }

    fun encode(share: CoachPlanShare): String {
        val json = toJson(share).toString()
        val b64 = Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return PREFIX + b64
    }

    fun decode(raw: String): DecodeResult {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(PREFIX)) {
            return DecodeResult.Error("不是有效的分享碼（缺少前綴）")
        }
        val b64 = trimmed.removePrefix(PREFIX)
        val jsonStr = try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            bytes.toString(Charsets.UTF_8)
        } catch (e: Exception) {
            return DecodeResult.Error("分享碼解析失敗（Base64）")
        }

        val obj = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            return DecodeResult.Error("分享碼解析失敗（JSON）")
        }

        val version = obj.optInt("version", 0)
        if (version != 1) {
            return DecodeResult.Error("不支援的版本：$version")
        }

        val shareId = obj.optString("shareId")
        val coachId = obj.optString("coachId")
        val planName = obj.optString("planName")
        val createdAtEpochMs = obj.optLong("createdAtEpochMs", 0L)
        if (shareId.isBlank() || coachId.isBlank() || planName.isBlank()) {
            return DecodeResult.Error("分享碼缺少必要欄位")
        }

        val exercisesArr = obj.optJSONArray("exercises") ?: JSONArray()
        val exercises = buildList {
            for (i in 0 until exercisesArr.length()) {
                val eo = exercisesArr.optJSONObject(i) ?: continue
                val name = eo.optString("name").trim()
                if (name.isBlank()) continue
                val reps = eo.opt("reps")?.let { if (it == JSONObject.NULL) null else eo.optInt("reps") }
                val sets = eo.opt("sets")?.let { if (it == JSONObject.NULL) null else eo.optInt("sets") }
                val weight = eo.opt("weight")?.let { if (it == JSONObject.NULL) null else eo.optDouble("weight").toFloat() }
                add(ExerciseEntry(name = name, reps = reps, sets = sets, weight = weight))
            }
        }

        return DecodeResult.Success(
            CoachPlanShare(
                version = 1,
                shareId = shareId,
                coachId = coachId,
                coachName = obj.optString("coachName").takeIf { it.isNotBlank() },
                planName = planName,
                exercises = exercises,
                createdAtEpochMs = createdAtEpochMs
            )
        )
    }

    private fun toJson(share: CoachPlanShare): JSONObject {
        val obj = JSONObject()
        obj.put("version", share.version)
        obj.put("shareId", share.shareId)
        obj.put("coachId", share.coachId)
        obj.put("coachName", share.coachName)
        obj.put("planName", share.planName)
        obj.put("createdAtEpochMs", share.createdAtEpochMs)
        val arr = JSONArray()
        share.exercises.forEach { e ->
            val eo = JSONObject()
            eo.put("name", e.name)
            eo.put("reps", e.reps)
            eo.put("sets", e.sets)
            eo.put("weight", e.weight)
            arr.put(eo)
        }
        obj.put("exercises", arr)
        return obj
    }
}

