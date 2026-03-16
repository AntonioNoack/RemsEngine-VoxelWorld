package me.anno.remcraft.entity

interface RideableEntity {
    val ridingHeight: Float
    var rider: MovingEntity?

    fun startRiding(player: PlayerEntity) {
        rider = player
        player.ridingOnEntity = this
    }

    fun exitRiding(player: PlayerEntity) {
        if (rider !== player) return
        rider = null
        player.ridingOnEntity = null
    }
}