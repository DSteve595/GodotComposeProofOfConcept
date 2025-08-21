package godot

import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import godot.annotation.RegisterClass
import godot.annotation.RegisterFunction
import godot.api.Control
import godot.api.Node
import godot.api.Time
import godot.core.connect
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration.Companion.microseconds

@RegisterClass
abstract class ComposeNode : Control() {

  internal val processStartChannel = Channel<Unit>()
  internal val processChannel = Channel<() -> Unit>(capacity = Channel.UNLIMITED)

  @RegisterFunction
  override fun _process(delta: Double) {
    processStartChannel.trySend(Unit)
    var process = processChannel.tryReceive()
    while (process.isSuccess) {
      process.getOrThrow().invoke()
      process = processChannel.tryReceive()
    }
  }
}

fun startNodeInTreeComposition(node: ComposeNode, content: @Composable () -> Unit) {
  require(node.isInsideTree()) { "Node must be inside the tree to create a composition" }
  val scope = createNodeCoroutineScope(node)
  val recomposer = Recomposer(scope.coroutineContext)
  val composition = Composition(
    NodeApplier(node),
    parent = recomposer, // TODO get composition context from ancestor node
  )
  composition.setContent(content)
  var snapshotHandle: ObserverHandle? = null

  scope.launch(start = CoroutineStart.UNDISPATCHED) {
    try {
      recomposer.runRecomposeAndApplyChanges()
    } finally {
      composition.dispose()
      snapshotHandle?.dispose()
    }
  }
  var applyScheduled = false
  snapshotHandle = Snapshot.registerGlobalWriteObserver {
    if (!applyScheduled) {
      applyScheduled = true
      scope.launch {
        applyScheduled = false
        Snapshot.sendApplyNotifications()
      }
    }
  }
}

private fun createNodeCoroutineScope(node: ComposeNode): CoroutineScope {
  val nodeProcessDispatcher = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
      node.processChannel.trySend { block.run() }
    }
  }
  val frameClock = object : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
      node.processStartChannel.receive()
      return suspendCoroutine { continuation ->
        continuation.resumeWith(runCatching { onFrame(Time.getTicksUsec().microseconds.inWholeNanoseconds) })
      }
    }
  }
  val scope = CoroutineScope(SupervisorJob() + nodeProcessDispatcher + frameClock)
  node.treeExiting.connect { scope.cancel() }
  return scope
}

internal class NodeApplier(root: Node) : AbstractApplier<Node>(root) {

  override fun insertTopDown(index: Int, instance: Node) {
  }

  override fun insertBottomUp(index: Int, instance: Node) {
    current.addChild(instance)
    if (index != current.getChildCount()) {
      current.moveChild(instance, index)
    }
  }

  override fun remove(index: Int, count: Int) {
    current.removeChild(current.getChild(index))
  }

  override fun move(from: Int, to: Int, count: Int) {
    if (from == to) return

    for (i in 0 until count) {
      // taken from compose's LayoutNode, no idea if it works lol
      // if "from" is after "to," the from index moves because we're inserting before it
      val fromIndex = if (from > to) from + i else from
      val toIndex = if (from > to) to + i else to + count - 2
      val child = current.getChild(fromIndex)
      current.moveChild(child, toIndex)
    }
  }

  override fun onClear() {
    repeat(root.getChildCount()) {
      root.removeChild(root.getChild(0))
    }
  }


}
