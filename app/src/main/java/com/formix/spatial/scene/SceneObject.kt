package com.formix.spatial.scene

class SceneObject(
    val id: Long,
    val type: PrimitiveType,
    var transform: Transform,
    var color: FloatArray // RGBA 0..1
) {
    var name: String = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} $id"
    var visible: Boolean = true
    var locked: Boolean = false
}
