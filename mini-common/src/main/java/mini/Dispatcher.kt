package mini

import java.util.HashMap
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList
import kotlin.collections.fold
import kotlin.collections.forEach
import kotlin.collections.getOrPut
import kotlin.collections.map
import kotlin.collections.minusAssign
import kotlin.collections.plusAssign
import kotlin.collections.remove
import kotlin.collections.sum
import kotlin.reflect.KClass


val actionCounter = AtomicInteger()

/**
 * Dispatch actions and subscribe to them in order to produce changes.
 */
class Dispatcher(var verifyThreads: Boolean = true) {
    val DEFAULT_PRIORITY: Int = 100

    val subscriptionCount: Int get() = subscriptionMap.values.map { it?.size ?: 0 }.sum()
    var dispatching: Boolean = false
        private set

    lateinit var actionReducer: ActionReducer
    private val subscriptionMap = HashMap<Class<*>, TreeSet<DispatcherSubscription<Any>>?>()
    private var subscriptionCounter = AtomicInteger()

    private val interceptors = ArrayList<Interceptor>()
    private val rootChain: Chain = object : Chain {
        override fun proceed(action: Action): Action {
            actionReducer.reduce(action)
            return action
        }
    }
    private var chain = rootChain
    private fun buildChain(): Chain {
        return interceptors.fold(rootChain)
        { chain, interceptor ->
            object : Chain {
                override fun proceed(action: Action): Action = interceptor(action, chain)
            }
        }
    }

    fun addInterceptor(interceptor: Interceptor) {
        synchronized(this) {
            interceptors += interceptor
            chain = buildChain()
        }
    }

    fun removeInterceptor(interceptor: Interceptor) {
        synchronized(this) {
            interceptors -= interceptor
            chain = buildChain()
        }
    }

    fun dispatch(action: Action) {
        if (verifyThreads) assertOnUiThread()
        synchronized(this) {
            try {
                if (dispatching) error("Can't dispatch actions while reducing state!")
                actionCounter.incrementAndGet()
                dispatching = true
                chain.proceed(action)
            } finally {
                dispatching = false
            }
        }
    }

    /**
     * Post an event that will dispatch the action on the Ui thread
     * and return immediately.
     */
    fun dispatchOnUi(action: Action) {
        onUi { dispatch(action) }
    }

    /**
     * Post and event that will dispatch the action on the Ui thread
     * and block until the dispatch is complete.
     *
     * Can't be called from the main thread.
     */
    fun dispatchOnUiSync(action: Action) {
        if (verifyThreads) assertNotOnUiThread()
        onUiSync { dispatch(action) }
    }

    fun <T : Any> subscribe(tag: KClass<T>, fn: (T) -> Unit = {}) = subscribe(DEFAULT_PRIORITY, tag, fn)

    fun <T : Any> subscribe(priority: Int,
                            tag: KClass<T>,
                            fn: (T) -> Unit = {}): DispatcherSubscription<T> {
        val subscription = DispatcherSubscription(
                this,
                subscriptionCounter.getAndIncrement(),
                priority,
                tag.java,
                fn)
        return registerInternal(subscription)
    }

    internal fun <T : Any> registerInternal(dispatcherSubscription: DispatcherSubscription<T>): DispatcherSubscription<T> {
        @Suppress("UNCHECKED_CAST")
        synchronized(this) {
            subscriptionMap.getOrPut(dispatcherSubscription.tag, {
                TreeSet({ a, b ->
                    val p = a.priority.compareTo(b.priority)
                    if (p == 0) a.id.compareTo(b.id)
                    else p
                })
            })!!.add(dispatcherSubscription as DispatcherSubscription<Any>)
        }
        return dispatcherSubscription
    }

    internal fun <T : Any> unregisterInternal(dispatcherSubscription: DispatcherSubscription<T>) {
        synchronized(this) {
            val set = subscriptionMap[dispatcherSubscription.tag] as? TreeSet<*>
            val removed = set?.remove(dispatcherSubscription) == true
            if (!removed) {
                Grove.w { "Failed to remove dispatcherSubscription, multiple dispose calls?" }
            }
        }
    }
}