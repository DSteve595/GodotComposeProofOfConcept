package godot

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import godot.core.Vector2
import godot.core.Vector2i

private val Vector2ToVector: TwoWayConverter<Vector2, AnimationVector2D> =
  TwoWayConverter(
    convertToVector = { AnimationVector2D(it.x.toFloat(), it.y.toFloat()) },
    convertFromVector = { Vector2(it.v1, it.v2) },
  )

val Vector2.Companion.VectorConverter: TwoWayConverter<Vector2, AnimationVector2D>
  get() = Vector2ToVector

private val Vector2iToVector: TwoWayConverter<Vector2i, AnimationVector2D> =
  TwoWayConverter(
    convertToVector = { AnimationVector2D(it.x.toFloat(), it.y.toFloat()) },
    convertFromVector = { Vector2i(it.v1, it.v2) },
  )

val Vector2i.Companion.VectorConverter: TwoWayConverter<Vector2i, AnimationVector2D>
  get() = Vector2iToVector
