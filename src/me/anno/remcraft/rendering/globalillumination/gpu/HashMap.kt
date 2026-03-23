package me.anno.remcraft.rendering.globalillumination.gpu

lateinit var ChunkData: IntArray

data class HashMap(val capacity: Int, val entryOffset: Int)

fun HashUtilMix(x: Int): Int {
    val h = x * -1640531527
    return h xor (h ushr 16)
}

fun HashMapGetKey(hashMap: HashMap, pos: Int): Int {
    return ChunkData[hashMap.entryOffset + pos]
}

fun HashMapGetValue(hashMap: HashMap, pos: Int): Int {
    return hashMap.entryOffset + hashMap.capacity + hashMap.entryOffset
}

fun HashMapGet(hashMap: HashMap, key: Int): Int {
    val mask = hashMap.capacity - 1
    var pos = HashUtilMix(key) and mask
    while (true) {
        val currKey = HashMapGetKey(hashMap, pos)
        if (currKey == 0) return -1
        if (currKey == key) return HashMapGetValue(hashMap, pos)
        pos = (pos + 1) and mask
    }
}

val gpuHashMap = """
        struct HashMap {
            int capacity;
            int entryOffset;
        };
        int HashUtilMix(int x) {
            uint h = uint(x * -1640531527);
            return int(h ^ (h >> 16));
        }
        int HashMapGetKey(HashMap hashMap, int pos) {
            return ChunkData[hashMap.entryOffset + pos];
        }
        int HashMapGetValue(HashMap hashMap, int pos) {
            return ChunkData[hashMap.entryOffset + hashMap.capacity + pos];
        }
        int HashMapGet(HashMap hashMap, int key) {
            int mask = hashMap.capacity - 1;
            
            /*for (int i=0; i<=mask; i++) {
                int currKey = HashMapGetKey(hashMap, i);
                if (currKey == key) return HashMapGetValue(hashMap, i);
            }
            return -1;*/
            
            int pos = HashUtilMix(key) & mask;
            while (true) {
                int currKey = HashMapGetKey(hashMap, pos);
                if (currKey == key) return HashMapGetValue(hashMap, pos);
                if (currKey == 0) return -1;
                pos = (pos + 1) & mask;
            }
        }
    """.trimIndent()
// todo one of our bugs is a hash-mismatch... what is being tested vs stored?
