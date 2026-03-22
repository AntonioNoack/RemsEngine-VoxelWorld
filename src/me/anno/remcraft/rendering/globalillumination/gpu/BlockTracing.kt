package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsY
import me.anno.remcraft.world.Index.bitsZ
import me.anno.remcraft.world.Index.maskX
import me.anno.remcraft.world.Index.maskY
import me.anno.remcraft.world.Index.maskZ
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4i
import kotlin.math.abs
import kotlin.math.min

fun HashChunkId(xi: Int, yi: Int, zi: Int): Int {
    val mask = 0x7fff_ffff
    return xi xor yi.and(mask).shl(11) xor zi.and(mask).shl(22)
}

fun HashChunkId(v: Vector3u): Int {
    val mask = 0x7fff_ffffu
    return (v.x xor v.y.and(mask).shl(11) xor v.z.and(mask).shl(22)).toInt()
}

fun GetBlockInChunk(chunkData: Int, blockPos: Vector3i): Int {
    val bx = blockPos.x and maskX
    val by = blockPos.y and maskY
    val bz = blockPos.z and maskZ
    val blockId = bx + (by shl bitsX) + (bz shl (bitsX + bitsY))
    return ChunkData[chunkData + blockId]
}

data class Result(
    val hitDistance: Float,
    val hitFace: Vector4i,
    val hitBlockId: Int
)

var chunkMap = -1
var maxSteps = 100

fun blockTracing(
    localStart: Vector3f,
    dir: Vector3f,
): Result? {
    if (abs(dir.x) < 1e-7) dir.x = 1e-7f
    if (abs(dir.y) < 1e-7) dir.y = 1e-7f
    if (abs(dir.z) < 1e-7) dir.z = 1e-7f
    val dirSign = sign(dir)
    val blockPos = ivec3(floor(localStart))
    val dist3 = (dirSign * 0.5f + 0.5f + vec3(blockPos) - localStart) / dir
    val invUStep = dirSign / dir
    var dist = 0f
    val faceCandidates = ivec3(
        if (dir.x < 0f) BlockSide.NX.ordinal else BlockSide.PX.ordinal,
        if (dir.y < 0f) BlockSide.NY.ordinal else BlockSide.PY.ordinal,
        if (dir.z < 0f) BlockSide.NZ.ordinal else BlockSide.PZ.ordinal
    );

    val dtf3 = localStart / dir
    val dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z))
    var faceId = if (dtf3.z == dtf1) faceCandidates.z
    else if (dtf3.y == dtf1) faceCandidates.y
    else faceCandidates.x

    var chunkId = uvec3(0xffffffffu)
    var chunkData = -1;

    var hitBlockId = 0
    var hitFace = ivec4(0)
    var hitDistance = -1f

    val chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);

    val dirSignI = ivec3(dirSign);
    for (i in 0 until maxSteps) {
        var nextDist = min(dist3.x, min(dist3.y, dist3.z))
        var continueTracing = true

        // check block is here:
        val newChunkId = uvec3(blockPos) shr ivec3(bitsX, bitsY, bitsZ);
        if (newChunkId != chunkId) {
            // if chunkId has changed, change chunk
            chunkData = HashMapGet(chunkHashMap, HashChunkId(newChunkId));
            chunkId = newChunkId;
        }

        if (chunkData != -1) {
            hitBlockId = GetBlockInChunk(chunkData, blockPos);
            continueTracing = hitBlockId == 0;
        }

        if (continueTracing) {
            if (nextDist == dist3.x) {
                blockPos.x += dirSignI.x;
                dist3.x += invUStep.x;
                faceId = faceCandidates.x;
            } else if (nextDist == dist3.y) {
                blockPos.y += dirSignI.y;
                dist3.y += invUStep.y;
                faceId = faceCandidates.y;
            } else {
                blockPos.z += dirSignI.z;
                dist3.z += invUStep.z;
                faceId = faceCandidates.z;
            }
            dist = nextDist;
        } else break
    }
    if (hitBlockId == 0) return null;

    hitDistance = dist;
    hitFace = ivec4(blockPos, faceId);

    return Result(hitDistance, hitFace, hitBlockId)
}

val blockTracing = """
        int HashChunkId(uvec3 chunkId) {
            return int(chunkId.x ^ (chunkId.y << 11) ^ (chunkId.z << 22));
        }
        int GetBlockInChunk(int chunkData, ivec3 blockPos) {
            blockPos = blockPos & ivec3($maskX,$maskY,$maskZ);
            int blockId = blockPos.x + (blockPos.y << $bitsX) + (blockPos.z << ${bitsX + bitsY});
            return ChunkData[chunkData + blockId];
        }
        bool blockTracing(
            vec3 localStart, vec3 dir,
            out float hitDistance,
            out ivec4 hitFace,
            out int hitBlockId
        ) {
            if (abs(dir.x) < 1e-7) dir.x = 1e-7;
            if (abs(dir.y) < 1e-7) dir.y = 1e-7;
            if (abs(dir.z) < 1e-7) dir.z = 1e-7;
            vec3 dirSign = sign(dir);
            ivec3 blockPos = ivec3(floor(localStart));
            vec3 dist3 = (dirSign*.5+.5 + vec3(blockPos) - localStart) / dir;
            vec3 invUStep = dirSign / dir;
            float nextDist, dist = 0.0;
            ivec3 faceCandidates = ivec3(
                dir.x < 0.0 ? ${BlockSide.NX.ordinal} : ${BlockSide.PX.ordinal},
                dir.y < 0.0 ? ${BlockSide.NY.ordinal} : ${BlockSide.PY.ordinal},
                dir.z < 0.0 ? ${BlockSide.NZ.ordinal} : ${BlockSide.PZ.ordinal}
            );
            
            vec3 dtf3 = localStart / dir;
            float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));
            int faceId = dtf3.z == dtf1 ? faceCandidates.z
                       : dtf3.y == dtf1 ? faceCandidates.y
                       :                  faceCandidates.x;
                       
            uvec3 chunkId = uvec3(0xffffffff);
            int chunkData = -1;
            
            hitBlockId = 0;
            hitFace = ivec4(0);
            hitDistance = -1.0;
            
            HashMap chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);
            
            ivec3 dirSignI = ivec3(dirSign);
            for (int i=0; i<maxSteps; i++) {
                nextDist = min(dist3.x, min(dist3.y, dist3.z));
                bool continueTracing = true;
                float skippingDist = 0.0;
                
                // check block is here:
                uvec3 newChunkId = uvec3(blockPos) >> ivec3($bitsX,$bitsY,$bitsZ);
                if (newChunkId != chunkId) {
                    // if chunkId has changed, change chunk
                    chunkData = HashMapGet(chunkHashMap, HashChunkId(newChunkId));
                    chunkId = newChunkId;
                }
                
                if (chunkData != -1) {
                    hitBlockId = GetBlockInChunk(chunkData, blockPos);
                    continueTracing = hitBlockId == 0;
                }
                
                /*if (skippingDist >= 1.0) {
                    // todo if a chunk is missing, skip it quickly...
                   vec3 blockPosF = floor(localStart + dir * (dist + skippingDist));
                   blockPos = ivec3(blockPosF);
                   dist3 = (dirSign*.5+.5 + blockPosF - localStart)/dir;
                   nextDist = min(dist3.x, min(dist3.y, dist3.z));
                   dist = nextDist;
               } else*/ if (continueTracing) {
                   if (nextDist == dist3.x) {
                       blockPos.x += dirSignI.x;
                       dist3.x += invUStep.x;
                       faceId = faceCandidates.x;
                   } else if (nextDist == dist3.y) {
                       blockPos.y += dirSignI.y;
                       dist3.y += invUStep.y;
                       faceId = faceCandidates.y;
                   } else {
                       blockPos.z += dirSignI.z;
                       dist3.z += invUStep.z;
                       faceId = faceCandidates.z;
                   }
                   dist = nextDist;
               } else break;
            }
            if (hitBlockId == 0) return false;
            
            hitDistance = dist;
            hitFace = ivec4(blockPos, faceId);
            
            return true;
        }
    """.trimIndent()
