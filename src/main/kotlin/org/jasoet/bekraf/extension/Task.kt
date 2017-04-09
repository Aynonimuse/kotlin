package org.jasoet.bekraf.extension

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.deferred


/**
 * [Documentation Here]
 *
 * @author Deny Prasetyo.
 */


inline fun <T> mongoTask(operation: ((T?, Throwable?) -> Unit) -> Unit): Promise<T, Exception> {
    val deferred = deferred<T, Exception>()

    val handler: (T?, Throwable?) -> Unit = { value, throwable ->
        if (throwable != null) {
            deferred.reject(Exception(throwable))
        } else if (value == null) {
            deferred.reject(Exception("Value is Null", throwable))
        } else {
            deferred.resolve(value)
        }
    }

    try {
        operation(handler)
    } catch (e: Exception) {
        deferred.reject(e)
    }

    return deferred.promise
}

inline fun <T> promiseTask(operation: (Handler<AsyncResult<T>>) -> Unit): Promise<T, Exception> {
    val deferred = deferred<T, Exception>()

    val handler = Handler<AsyncResult<T>> {
        if (it.succeeded()) {
            deferred.resolve(it.result())
        } else {
            deferred.reject(Exception(it.cause()))
        }
    }

    try {
        operation(handler)
    } catch (e: Exception) {
        deferred.reject(e)
    }

    return deferred.promise
}



