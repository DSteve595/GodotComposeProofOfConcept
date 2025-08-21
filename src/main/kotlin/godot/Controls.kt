package godot

import androidx.compose.runtime.*
import godot.api.*
import godot.core.Vector2
import godot.core.connect

// Eventually autogenerate this (one property for each of Control's properties)
interface ControlProps {
  var position: Vector2
}

internal interface ControlPropsImpl<T : Control> : ControlProps {
  fun Updater<T>.updateNodeProperties()
  fun clear()
}

// Eventually autogenerate this (do we "elevate" properties like `text` to give them shortcuts args?
// If so, how do we decide which to elevate?
interface LabelProps : ControlProps {
  var text: String
}

internal class LabelPropsImpl : LabelProps, ControlPropsImpl<Label> {

  // should these be set to the node's current values so that the getter is usable?
  // or should we turn them into setter functions so they can't be read?

  override var position: Vector2 = Vector2.ZERO
  override var text: String = ""

  override fun Updater<Label>.updateNodeProperties() {
    set(position) { this.setPosition(it) }
    set(text) { this.text = it }
  }

  override fun clear() {
    position = Vector2.ZERO
    text = ""
  }
}

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