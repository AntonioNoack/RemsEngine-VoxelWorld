# Voxel World in Rem's Engine

This is a sample for [Rem's Engine](https://github.com/AntonioNoack/RemsEngine) to show how you could implement a blocky world like in Minecraft.
It's just a sample, so feel free to modify it!

![First Trees within this Mod](/progress/Trees.png "Overview of this sample mod")

## World & Chunks

The system is split into simulation and visuals, so it could potentially used for multiplayer in the future.

A world consists of multiple dimensions. A dimension is made out of block and entities.
Chunks group these blocks and entities, so they can be stored and handled more efficiently.

In this mod, the chunks are 3d chunks.

## Chunk System

The chunk system of this mod uses the built-in ChunkSystem class. The visuals are stored in the ECS system in a flat hierarchy.
