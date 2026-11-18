package org.example.project.data.ads

/** Android has no tracking prompt; the advertising ID is governed by the system setting. */
internal actual suspend fun requestAdTrackingAuthorization() = Unit
