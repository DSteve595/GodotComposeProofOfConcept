package godot

import androidx.compose.runtime.*
import godot.core.Signal1
import godot.core.callable1

@Composable
inline fun <reified T : R, R> Signal1<T>.connectAsState(initialValue: R): State<R> {
  val state = remember { mutableStateOf(initialValue) }
  DisposableEffect(this) {
    val callable = callable1<T, Unit> { state.value = it }
    connect(callable)
    onDispose { disconnect(callable) }
  }
  return state
}