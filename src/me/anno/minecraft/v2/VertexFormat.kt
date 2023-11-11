package me.anno.minecraft.v2

import me.anno.ecs.components.mesh.MeshVertexData
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

val blockAttributes = listOf(
    Attribute("coords", 3),
    Attribute("colors0", AttributeType.UINT8_NORM, 4)
)

val blockAttributes2 = listOf(
    Attribute("coords", 3),
    Attribute("blockIndex", AttributeType.SINT32, 1, true)
)

val blockVertexData = MeshVertexData(
    listOf(
        ShaderStage(
            "coords", listOf(
                Variable(GLSLType.V3F, "coords", VariableMode.ATTR),
                Variable(GLSLType.V1I, "blockIndex", VariableMode.ATTR),
                Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
            ), "localPosition = coords;\n"
        )
    ),
    listOf(
        ShaderStage(
            "nor", listOf(
                Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
            ), "normal = vec3(0.0); tangent = vec4(0.0);\n"
        )
    ),
    MeshVertexData.DEFAULT.loadColors,
    MeshVertexData.DEFAULT.loadMotionVec,
    listOf(
        // calculate normals using cross product
        ShaderStage(
            "px-nor", listOf(
                Variable(GLSLType.V3F, "finalPosition"),
                Variable(GLSLType.V3F, "normal", VariableMode.OUT)
            ), "normal = normalize(cross(dFdx(finalPosition), dFdy(finalPosition)));\n"
        )
    ),
)
