package godot

import androidx.compose.runtime.*
import godot.api.*
import godot.core.connect

/**
 * Shortcut for Label with props { text = "" }
 */
@Composable
fun Label(
  text: String,
  props: (LabelProps.() -> Unit)? = null,
) {
  Label(
    props = {
      props?.invoke(this)
      this.text = text
    }
  )
}

@Composable
fun Label(props: LabelProps.() -> Unit) {
  ControlNode(
    factory = { Label() },
    propsImpl = remember { LabelPropsImpl() },
    propsBlock = props,
  )
}

@Composable
internal inline fun <reified T : Control, PropsImpl : ControlPropsImpl<T>> ControlNode(
  noinline factory: () -> T,
  propsImpl: PropsImpl,
  propsBlock: @DisallowComposableCalls PropsImpl.() -> Unit,
) {
  ComposeNode<T, NodeApplier>(
    factory = factory,
    update = {
      propsImpl.clear()
      propsBlock(propsImpl)
      with(propsImpl) { updateNodeProperties() }
    },
  )
}

//@Composable
//fun Label(
//  text: String,
//  position: Vector2 = Vector2.ZERO,
//) {
//  ComposeNode<Label, NodeApplier>(
//    factory = { Label() },
//    update = {
//      set(text) { this.text = it }
//      set(position) { this.setPosition(it) }
//    },
//  )
//}

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