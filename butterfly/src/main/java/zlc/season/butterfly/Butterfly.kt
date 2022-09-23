package zlc.season.butterfly

import android.app.Activity
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

object Butterfly {
    const val RAW_SCHEME = "butterfly_scheme"

    private val EMPTY_LAMBDA: (Bundle) -> Unit = {}

    fun agile(scheme: String): AgileRequest {
        val realScheme = parseScheme(scheme)
        return ButterflyCore.queryAgile(realScheme).apply {
            val params = parseSchemeParams(scheme)
            bundle.putAll(bundleOf(*params))
        }
    }

    fun AgileRequest.params(vararg pair: Pair<String, Any?>): AgileRequest {
        return apply {
            bundle.putAll(bundleOf(*pair))
        }
    }

    fun AgileRequest.params(bundle: Bundle): AgileRequest {
        return apply { bundle.putAll(bundle) }
    }

    fun AgileRequest.skipGlobalInterceptor(): AgileRequest {
        return copy(enableGlobalInterceptor = false)
    }

    fun AgileRequest.addInterceptor(interceptor: ButterflyInterceptor): AgileRequest {
        return apply { interceptorController.addInterceptor(interceptor) }
    }

    fun AgileRequest.addInterceptor(interceptor: suspend (AgileRequest) -> Unit): AgileRequest {
        return apply {
            interceptorController.addInterceptor(DefaultButterflyInterceptor(interceptor))
        }
    }

    fun AgileRequest.container(containerViewId: Int): AgileRequest {
        return copy(fragmentConfig = fragmentConfig.copy(containerViewId = containerViewId))
    }

    fun AgileRequest.tag(tag: String): AgileRequest {
        return copy(fragmentConfig = fragmentConfig.copy(tag = tag))
    }

    fun AgileRequest.clearTop(): AgileRequest {
        return copy(
            activityConfig = activityConfig.copy(clearTop = true),
            fragmentConfig = fragmentConfig.copy(clearTop = true)
        )
    }

    fun AgileRequest.singleTop(): AgileRequest {
        return copy(
            activityConfig = activityConfig.copy(singleTop = true),
            fragmentConfig = fragmentConfig.copy(singleTop = true)
        )
    }

    fun AgileRequest.disableBackStack(): AgileRequest {
        return copy(fragmentConfig = fragmentConfig.copy(enableBackStack = false))
    }

    fun AgileRequest.addFlag(flag: Int): AgileRequest {
        return copy(activityConfig = activityConfig.copy(flags = activityConfig.flags or flag))
    }

    fun AgileRequest.enterAnim(enterAnim: Int = 0): AgileRequest {
        return copy(
            activityConfig = activityConfig.copy(enterAnim = enterAnim),
            fragmentConfig = fragmentConfig.copy(enterAnim = enterAnim)
        )
    }

    fun AgileRequest.exitAnim(exitAnim: Int = 0): AgileRequest {
        return copy(
            activityConfig = activityConfig.copy(exitAnim = exitAnim),
            fragmentConfig = fragmentConfig.copy(exitAnim = exitAnim)
        )
    }

    fun AgileRequest.flow(): Flow<Unit> {
        return ButterflyCore.dispatchAgile(copy(needResult = false)).flatMapConcat { flowOf(Unit) }
    }

    fun AgileRequest.resultFlow(): Flow<Result<Bundle>> {
        return ButterflyCore.dispatchAgile(copy(needResult = true))
    }

    fun AgileRequest.carry(
        onError: (Throwable) -> Unit = {},
        onResult: (Bundle) -> Unit = EMPTY_LAMBDA
    ) {
        carry(ButterflyHelper.scope, onError, onResult)
    }

    fun AgileRequest.carry(
        scope: CoroutineScope = ButterflyHelper.scope,
        onError: (Throwable) -> Unit = {},
        onResult: (Bundle) -> Unit = EMPTY_LAMBDA
    ) {
        if (onResult == EMPTY_LAMBDA) {
            flow().launchIn(scope)
        } else {
            resultFlow().onEach {
                if (it.isSuccess) {
                    onResult(it.getOrDefault(Bundle()))
                } else {
                    onError(it.exceptionOrNull() ?: Throwable())
                }
            }.launchIn(scope)
        }
    }

    fun retreat(vararg result: Pair<String, Any?>): Boolean {
        return ButterflyCore.dispatchRetreat(Any::class.java, bundleOf(*result))
    }

    fun retreatDialog(vararg result: Pair<String, Any?>): Boolean {
        return ButterflyCore.dispatchRetreat(DialogFragment::class.java, bundleOf(*result))
    }

    fun retreatFragment(vararg result: Pair<String, Any?>): Boolean {
        return ButterflyCore.dispatchRetreat(Fragment::class.java, bundleOf(*result))
    }

    fun retreatFragmentCount(): Int {
        return ButterflyCore.getRetreatCount(Fragment::class.java)
    }

    fun retreatDialogCount(): Int {
        return ButterflyCore.getRetreatCount(DialogFragment::class.java)
    }

    fun canRetreat(): Boolean {
        return canRetreatDialog() || canRetreatFragment()
    }

    fun canRetreatFragment(): Boolean {
        return retreatFragmentCount() > 0
    }

    fun canRetreatDialog(): Boolean {
        return retreatDialogCount() > 0
    }

    fun Activity.retreat(vararg result: Pair<String, Any?>): Boolean {
        return ButterflyCore.dispatchRetreatDirectly(javaClass, this, bundleOf(*result))
    }

    fun Fragment.retreat(vararg result: Pair<String, Any?>): Boolean {
        return ButterflyCore.dispatchRetreatDirectly(javaClass, this, bundleOf(*result))
    }

    fun DialogFragment.retreat(vararg result: Pair<String, Any?>): Boolean {
        return ButterflyCore.dispatchRetreatDirectly(javaClass, this, bundleOf(*result))
    }

    val EVADE_LAMBDA: (String, Class<*>) -> Any = { identity, cls ->
        val real = identity.ifEmpty { cls.simpleName }
        var request = ButterflyCore.queryEvade(real)
        if (request.className.isEmpty()) {
            request = request.copy(className = cls.name)
        }
        ButterflyCore.dispatchEvade(request)
    }

    inline fun <reified T> evade(
        identity: String = "",
        noinline func: (String, Class<*>) -> Any = EVADE_LAMBDA
    ): T {
        return func(identity, T::class.java) as T
    }
}