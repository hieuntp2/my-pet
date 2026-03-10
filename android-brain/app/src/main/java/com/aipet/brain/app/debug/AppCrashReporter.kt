package com.aipet.brain.app.debug

import android.content.Context
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

data class AppCrashReport(
    val occurredAtMs: Long,
    val threadName: String,
    val source: String,
    val summary: String,
    val stackTrace: String
)

object AppCrashReporter {
    private const val TAG = "AppCrashReporter"
    private const val PREFS_NAME = "app_crash_report"
    private const val KEY_OCCURRED_AT_MS = "occurred_at_ms"
    private const val KEY_THREAD_NAME = "thread_name"
    private const val KEY_SOURCE = "source"
    private const val KEY_SUMMARY = "summary"
    private const val KEY_STACK_TRACE = "stack_trace"

    @Volatile
    private var installed = false

    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install(context: Context) {
        if (installed) {
            return
        }
        synchronized(this) {
            if (installed) {
                return
            }
            val appContext = context.applicationContext
            previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                persist(
                    context = appContext,
                    throwable = throwable,
                    source = "uncaught_exception",
                    threadName = thread.name
                )
                previousHandler?.uncaughtException(thread, throwable) ?: run {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    exitProcess(10)
                }
            }
            installed = true
        }
    }

    fun persistHandledException(
        context: Context,
        throwable: Throwable,
        source: String
    ): AppCrashReport {
        return persist(
            context = context.applicationContext,
            throwable = throwable,
            source = source,
            threadName = Thread.currentThread().name
        )
    }

    fun latest(context: Context): AppCrashReport? {
        val preferences = preferences(context.applicationContext)
        val stackTrace = preferences.getString(KEY_STACK_TRACE, null) ?: return null
        val occurredAtMs = preferences.getLong(KEY_OCCURRED_AT_MS, 0L)
        val threadName = preferences.getString(KEY_THREAD_NAME, "-").orEmpty()
        val source = preferences.getString(KEY_SOURCE, "-").orEmpty()
        val summary = preferences.getString(KEY_SUMMARY, "-").orEmpty()
        return AppCrashReport(
            occurredAtMs = occurredAtMs,
            threadName = threadName,
            source = source,
            summary = summary,
            stackTrace = stackTrace
        )
    }

    fun clear(context: Context) {
        preferences(context.applicationContext).edit().clear().apply()
    }

    private fun persist(
        context: Context,
        throwable: Throwable,
        source: String,
        threadName: String
    ): AppCrashReport {
        val occurredAtMs = System.currentTimeMillis()
        val summary = buildString {
            append(throwable.javaClass.name)
            if (!throwable.message.isNullOrBlank()) {
                append(": ")
                append(throwable.message)
            }
        }
        val stackTrace = throwable.toStackTraceString()
        preferences(context).edit()
            .putLong(KEY_OCCURRED_AT_MS, occurredAtMs)
            .putString(KEY_THREAD_NAME, threadName)
            .putString(KEY_SOURCE, source)
            .putString(KEY_SUMMARY, summary)
            .putString(KEY_STACK_TRACE, stackTrace)
            .apply()
        Log.e(TAG, "Captured exception from $source on thread=$threadName", throwable)
        return AppCrashReport(
            occurredAtMs = occurredAtMs,
            threadName = threadName,
            source = source,
            summary = summary,
            stackTrace = stackTrace
        )
    }

    private fun Throwable.toStackTraceString(): String {
        val writer = StringWriter()
        PrintWriter(writer).use { printWriter ->
            printStackTrace(printWriter)
        }
        return writer.toString()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
