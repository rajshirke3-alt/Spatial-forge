package com.formix.spatial.scene

class Scene {
    val objects = ArrayList<SceneObject>()
    var selectedId: Long? = null
    private var nextId = 1L

    fun newId(): Long = nextId++

    fun bumpIdCounterPast(id: Long) {
        if (id >= nextId) nextId = id + 1
    }

    fun add(obj: SceneObject) {
        objects.add(obj)
    }

    fun remove(id: Long) {
        objects.removeAll { it.id == id }
        if (selectedId == id) selectedId = null
    }

    fun find(id: Long?): SceneObject? = objects.firstOrNull { it.id == id }

    fun selected(): SceneObject? = find(selectedId)

    fun clear() {
        objects.clear()
        selectedId = null
        nextId = 1L
    }
}
