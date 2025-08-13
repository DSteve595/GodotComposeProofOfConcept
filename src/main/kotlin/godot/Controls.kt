package godot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import godot.api.Button
import godot.api.HBoxContainer
import godot.api.Label
import godot.api.VBoxContainer
import godot.core.connect

@Composable
fun Label(
  text: String,
) {
  ComposeNode<Label, NodeApplier>(
    factory = { Label() },
    update = {
      set(text) { this.text = it }
    },
  )
}

@Composable
fun Button(
  onPress: () -> Unit,
  text: String,
) {
  val updatedOnPress by rememberUpdatedState(onPress)
  ComposeNode<Button, NodeApplier>(
    factory = {
      Button().apply {
        pressed.connect { updatedOnPress() }
      }
    },
    update = {
      set(text) { this.text = it }
    },
  )
}

@Composable
fun VBoxContainer(
  content: @Composable () -> Unit,
) {
  ComposeNode<VBoxContainer, NodeApplier>(
    factory = { VBoxContainer() },
    update = {},
    content = content,
  )
}

@Composable
fun HBoxContainer(
  content: @Composable () -> Unit,
) {
  ComposeNode<HBoxContainer, NodeApplier>(
    factory = { HBoxContainer() },
    update = {},
    content = content,
  )
}