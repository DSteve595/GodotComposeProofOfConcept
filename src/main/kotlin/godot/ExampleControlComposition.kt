package godot

import androidx.compose.runtime.*
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.global.GD
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

@RegisterClass
class ExampleControlComposition : Control() {

  @RegisterFunction
  override fun _enterTree() {
    GD.print("_enterTree")
    startNodeInTreeComposition(this) {
      GD.print("composing")

      LaunchedEffect(Unit) {
        while (isActive) {
          delay(1.seconds)
          GD.print("LaunchedEffect fire")
        }
      }

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
      }
    }
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
