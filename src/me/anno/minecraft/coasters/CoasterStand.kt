package me.anno.minecraft.coasters

import org.joml.Quaternionf
import org.joml.Vector3d

// todo block type, on which coaster rails can be placed:
//  first place and rotate stands, when link them with rails
// todo should be connectable to normal rails

class CoasterStand(
    val position: Vector3d,
    val rotation: Quaternionf,
) {
    val direction = rotation.transform(Vector3d(0.0, 0.0, 1.0))
    val rightDirection = rotation.transform(Vector3d(1.0, 0.0, 1.0))
    val upDirection = rotation.transform(Vector3d(0.0, 1.0, 0.0))
}