package mini

class Resource<out T>
@PublishedApi internal constructor(val value: Any?) {

    val isSuccess: Boolean get() = !isLoading && !isFailure
    val isFailure: Boolean get() = value is Failure
    val isLoading: Boolean get() = value is Loading<*>

    internal class Failure(val exception: Throwable)
    internal class Loading<U>(val value: U? = null)

    fun getOrNull(): T? =
        when {
            isSuccess -> value as T?
            else -> null
        }

    fun exceptionOrNull(): Throwable? =
        when (value) {
            is Failure -> value.exception
            else -> null
        }

    override fun toString(): String =
        when (value) {
            is Failure -> value.toString() // "Failure($exception)"
            else -> "Success($value)"
        }

    companion object {
        fun <T> success(value: T): Resource<T> {
            return Resource(value)
        }

        fun <T> failure(exception: Throwable): Resource<T> {
            return Resource(Failure(exception))
        }

        fun <T> loading(value: T? = null): Resource<T> {
            return Resource(Loading(value))
        }
    }

    fun onSuccess(action: (data: T) -> Unit): Resource<T> {
        if (isSuccess) action(value as T)
        return this
    }

    fun onFailure(action: (throwable: Throwable) -> Unit): Resource<T> {
        if (isFailure) action((value as Failure).exception)
        return this
    }

    fun onLoading(action: (data: T) -> Unit): Resource<T> {
        if (isLoading) action(value as T)
        return this
    }
}

inline fun <T, R> Resource<T>.map(crossinline transform: (data: T) -> R): Resource<R> {
    if (isSuccess) return Resource.success(transform(value as T))
    return Resource(value)
}