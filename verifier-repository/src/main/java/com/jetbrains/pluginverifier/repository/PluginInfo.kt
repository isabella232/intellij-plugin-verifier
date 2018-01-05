package com.jetbrains.pluginverifier.repository

import java.net.URL
import java.util.*

/**
 * Identifier of an abstract plugin, which may
 * represent either a [plugin from the Plugin Repository] [UpdateInfo],
 * or a [locally stored plugin] [com.jetbrains.pluginverifier.repository.local.LocalPluginInfo].
 */
open class PluginInfo(
    val pluginId: String,

    val version: String,

    val repositoryURL: URL
) {

  open val presentableName: String = "$pluginId $version"

  final override fun equals(other: Any?) = other is PluginInfo &&
      pluginId == other.pluginId && version == other.version && repositoryURL == other.repositoryURL

  final override fun hashCode() = Objects.hash(pluginId, version, repositoryURL)

  final override fun toString() = presentableName

}