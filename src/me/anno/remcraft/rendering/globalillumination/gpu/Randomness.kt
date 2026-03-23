package me.anno.remcraft.rendering.globalillumination.gpu

val randomness = "" +
        "uint createSeed(uint baseSeed, uint id0, uint id1) {\n" +
        "   uint s = baseSeed;\n" +
        "   s ^= id0 * 0x9E3779B9u;\n" + // golden ratio prime
        "   s ^= id1 * 0x85EBCA6Bu;\n" +
        // final avalanche (bit mixing)
        "   s ^= (s >> 16);\n" +
        "   s *= 0x7FEB352Du;\n" +
        "   s ^= (s >> 15);\n" +
        "   s *= 0x846CA68Bu;\n" +
        "   s ^= (s >> 16);\n" +
        "   return s;\n" +
        "}\n" +

        "uint nextSeed(inout uint s) {\n" +
        "   uint state = s;\n" +
        "   s = state * 747796405u + 2891336453u; \n" +// LCG step
        "   uint word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;\n" +
        "   return (word >> 22u) ^ word;\n" +
        "}\n" +

        "float nextFloat(inout uint s) {\n" +
        "    return float(nextSeed(s)) * ${1.0 / ((1L shl 32) - 1.0)};\n" +
        "}\n"