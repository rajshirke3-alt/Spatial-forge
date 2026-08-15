package com.formix.spatial.io

import android.content.Context
import com.formix.spatial.scene.PrimitiveType
import com.formix.spatial.scene.Scene
import com.formix.spatial.scene.SceneObject
import com.formix.spatial.scene.Transform
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Native project format (".f3dproject" per spec). Phase 1 stores geometry + transforms +
 * color + scene hierarchy (flat list, no groups yet). Single autosave-style slot for now;
 * multi-project browsing is a later phase per the spec's roadmap.
 */
object ProjectSerializer {

    private const val FILE_NAME = "current_project.f3dproject"

    fun save(context: Context, scene: Scene): Boolean {
        return try {
            val root = JSONObject()
            root.put("formatVersion", 1)
            val arr = JSONArray()
            for (obj in scene.objects) {
                val o = JSONObject()
                o.put("id", obj.id)
                o.put("type", obj.type.name)
                o.put("name", obj.name)
                o.put("visible", obj.visible)
                o.put("locked", obj.locked)
                o.put("px", obj.transform.px); o.put("py", obj.transform.py); o.put("pz", obj.transform.pz)
                o.put("rx", obj.transform.rx); o.put("ry", obj.transform.ry); o.put("rz", obj.transform.rz)
                o.put("sx", obj.transform.sx); o.put("sy", obj.transform.sy); o.put("sz", obj.transform.sz)
                o.put("r", obj.color[0]); o.put("g", obj.color[1]); o.put("b", obj.color[2]); o.put("a", obj.color[3])
                arr.put(o)
            }
            root.put("objects", arr)

            File(context.filesDir, FILE_NAME).writeText(root.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    fun hasSavedProject(context: Context): Boolean = File(context.filesDir, FILE_NAME).exists()

    fun load(context: Context, scene: Scene): Boolean {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return false
        return try {
            val root = JSONObject(file.readText())
            val arr = root.getJSONArray("objects")
            scene.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.getLong("id")
                val transform = Transform(
                    px = o.getDouble("px").toFloat(), py = o.getDouble("py").toFloat(), pz = o.getDouble("pz").toFloat(),
                    rx = o.getDouble("rx").toFloat(), ry = o.getDouble("ry").toFloat(), rz = o.getDouble("rz").toFloat(),
                    sx = o.getDouble("sx").toFloat(), sy = o.getDouble("sy").toFloat(), sz = o.getDouble("sz").toFloat()
                )
                val color = floatArrayOf(
                    o.getDouble("r").toFloat(), o.getDouble("g").toFloat(),
                    o.getDouble("b").toFloat(), o.getDouble("a").toFloat()
                )
                val obj = SceneObject(id, PrimitiveType.valueOf(o.getString("type")), transform, color)
                obj.name = o.optString("name", obj.name)
                obj.visible = o.optBoolean("visible", true)
                obj.locked = o.optBoolean("locked", false)
                scene.add(obj)
                scene.bumpIdCounterPast(id)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
