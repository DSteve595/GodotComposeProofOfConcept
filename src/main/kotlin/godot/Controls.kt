package godot

import androidx.compose.runtime.*
import godot.api.*
import godot.core.connect

@Composable
internal inline fun <reified T : Control, PropsImpl : ControlPropsImpl<T>> ControlNode(
  noinline factory: () -> T,
  propsImpl: PropsImpl,
  propsBlock: @DisallowComposableCalls PropsImpl.() -> Unit,
  noinline content: (@Composable () -> Unit)?,
) {
  ComposeNode<T, NodeApplier>(
    factory = factory,
    update = {
      propsImpl.clear()
      propsBlock(propsImpl)
      with(propsImpl) { updateNodeProperties() }
    },
    content = { content?.invoke() },
  )
}

/**
 * Shortcut for Label with props { text = "" }
 */
@Composable
fun Label(
  text: String,
  props: LabelProps.() -> Unit = { },
) {
  Label(
    props = {
      props(this)
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
    content = null,
  )
}

@Composable
fun Button(
  onPress: () -> Unit,
  text: String,
  props: ButtonProps.() -> Unit = { },
) {
  val updatedOnPress by rememberUpdatedState(onPress)
  ControlNode(
    factory = {
      Button().apply {
        pressed.connect { updatedOnPress() }
      }
    },
    propsImpl = remember { ButtonPropsImpl() },
    propsBlock = {
      props(this)
      this.text = text
    },
    content = null,
  )
}
