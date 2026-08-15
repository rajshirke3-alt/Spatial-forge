package com.formix.spatial.history

import com.formix.spatial.scene.Scene
import com.formix.spatial.scene.SceneObject
import com.formix.spatial.scene.Transform

interface Command {
    fun apply()
    fun revert()
}

class AddObjectCommand(private val scene: Scene, private val obj: SceneObject) : Command {
    override fun apply() {
        scene.add(obj)
        scene.selectedId = obj.id
    }
    override fun revert() {
        scene.remove(obj.id)
    }
}

class DeleteObjectCommand(private val scene: Scene, private val obj: SceneObject) : Command {
    override fun apply() {
        scene.remove(obj.id)
    }
    override fun revert() {
        scene.add(obj)
        scene.selectedId = obj.id
    }
}

class TransformCommand(
    private val scene: Scene,
    private val objectId: Long,
    private val before: Transform,
    private val after: Transform
) : Command {
    override fun apply() {
        scene.find(objectId)?.transform = after.copy2()
    }
    override fun revert() {
        scene.find(objectId)?.transform = before.copy2()
    }
}
