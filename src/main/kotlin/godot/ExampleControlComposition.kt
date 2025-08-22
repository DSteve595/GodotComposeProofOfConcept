package godot

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.core.Color
import godot.core.Vector2
import godot.global.GD
import kotlinx.coroutines.isActive

@RegisterClass
abstract class ComposeControlNode : ComposeNode() {

  @Composable
  abstract fun Content()

  @RegisterFunction
  override fun _enterTree() {
    GD.print("_enterTree")
    startNodeInTreeComposition(this) {
      GD.print("composing")
      Content()
    }
  }
}

@RegisterClass
class ExampleControlComposition : ComposeControlNode() {

  @Composable
  override fun Content() {
    ExampleContent()
  }
}

@Composable
private fun ExampleContent() {
  VBoxContainer {
    Label("Hello")
    var labels by remember { mutableStateOf(0) }
    HBoxContainer {
      Button(onPress = { labels++ }, text = "Add")
      Button(onPress = { labels-- }, text = "Remove")
    }
    repeat(labels) {
      val frameCounter by rememberFrameCounterAsState()
      Label("Hello! Alive for $frameCounter frames")
      DisposableEffect(Unit) {
        onDispose {
          GD.print("Disposing label")
        }
      }
    }
    Label(
      "I've got props",
      props = {
        uppercase = true
        modulate = Color.aqua
      }
    )
    val animatableOffset = remember { Animatable(Vector2.ZERO, Vector2.VectorConverter) }
    LaunchedEffect(Unit) {
      while (isActive) {
        animatableOffset.animateTo(Vector2((0..200).random(), (0..200).random()))
      }
    }
    Label(
      "animating",
      props = {
        position = Vector2(animatableOffset.value.x, animatableOffset.value.y)
      },
    )
  }
}

@Composable
private fun rememberFrameCounterAsState(): State<Long> {
  return produceState(initialValue = 0) {
    while (isActive) {
      withFrameNanos {
        value++
      }
    }
  }
}
