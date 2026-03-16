package me.anno.remcraft.coasters

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import me.anno.utils.types.Booleans.toFloat

fun main() {
    testSceneWithUI(
        "Minecart", Entity()
            .add(object : Component(), OnUpdate {
                override fun onUpdate() {
                    val mc = getComponent(Minecart::class)!!
                    mc.animPosition = Time.gameTime.toFloat() * 5f
                    val si = Input.isLeftDown.toFloat() - Input.isRightDown.toFloat()
                    mc.steering = mix(mc.steering, 0.3f * si, dtTo01(Time.deltaTime.toFloat() * 8f))
                }
            })
            .add(Minecart())
    )
}