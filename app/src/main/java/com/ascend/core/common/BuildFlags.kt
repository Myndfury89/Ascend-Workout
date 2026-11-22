package com.ascend.core.common

/**
 * Variant flags for the running process. [isDebug] mirrors `BuildConfig.DEBUG`: it is true for
 * debug builds and false for release. Features that must not ship (the class-awakening debug
 * override) consult this injected value so tests can simulate a release build.
 */
data class BuildFlags(
    val isDebug: Boolean,
)
