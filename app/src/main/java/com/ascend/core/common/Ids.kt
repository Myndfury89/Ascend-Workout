package com.ascend.core.common

import java.util.UUID

/**
 * The app works fully offline with a single local profile by default. A synced
 * account (Phase 11) can replace this id without schema changes.
 */
const val LOCAL_USER_ID: String = "local-player"

/** Generate a new opaque entity id. */
fun newId(): String = UUID.randomUUID().toString()
