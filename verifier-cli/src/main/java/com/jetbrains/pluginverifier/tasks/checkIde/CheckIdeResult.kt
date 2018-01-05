package com.jetbrains.pluginverifier.tasks.checkIde

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.parameters.filtering.PluginIdAndVersion
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.tasks.TaskResult

data class CheckIdeResult(val ideVersion: IdeVersion,
                          val results: List<Result>,
                          val excludedPlugins: List<PluginIdAndVersion>,
                          val noCompatibleUpdatesProblems: List<MissingCompatibleUpdate>) : TaskResult
