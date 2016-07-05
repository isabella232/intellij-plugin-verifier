package com.jetbrains.pluginverifier.api

import com.intellij.structure.domain.Ide
import com.intellij.structure.domain.Plugin
import com.intellij.structure.errors.IncorrectPluginException
import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.verifiers.VContext
import com.jetbrains.pluginverifier.verifiers.Verifiers
import org.slf4j.LoggerFactory
import java.io.IOException


/**
 * @author Sergey Patrikeev
 */
object VManager {

  private val LOG = LoggerFactory.getLogger(VManager::class.java)

  /**
   * Perform the verification according to passed parameters.
   *
   * The parameters consist of the _(ide, plugin)_ pairs.
   * Every plugin is checked against the corresponding IDE.
   * For every such pair the method returns a result of the verification:
   * normally every result consists of the found binary problems (the main task of the Verifier), if they exist.
   * But there could be the verification errors (the most typical are due to missing plugin mandatory dependencies,
   * invalid plugin class-files or whatever other reasons).
   * Thus you should check the type of the _(ide, plugin)_ pairs results (see [VResult]) .
   *
   * @return the verification results
   * @throws InterruptedException if the verification was cancelled
   * @throws RuntimeException if unexpected errors occur: e.g. the IDE is broken, or the Repository doesn't respond.
   */
  @Throws(InterruptedException::class)
  fun verify(params: VParams): VResults {

    LOG.info("Verifying the plugins according to $params")

    val time = System.currentTimeMillis()

    val results = arrayListOf<VResult>()

    //IOException is propagated. Auto-close the JDK resolver (we have just created it)
    VParamsCreator.createJdkResolver(params.jdkDescriptor).use { runtimeResolver ->

      //Group by IDE to reduce the Ide-Resolver creations number.
      params.pluginsToCheck.groupBy { it.second }.entries.forEach { ideToPlugins ->

        checkCancelled()

        LOG.debug("Creating Resolver for ${ideToPlugins.key}")

        val ide: Ide
        try {
          ide = VParamsCreator.getIde(ideToPlugins.key)
        } catch(ie: InterruptedException) {
          throw ie
        } catch(e: Exception) {
          //IDE errors are propagated. We assume the IDE-s are correct while the plugins may not be so.
          throw RuntimeException("Failed to create IDE instance for ${ideToPlugins.key}")
        }

        //we must not close the IDE Resolver coming from the caller
        val closeIdeResolver: Boolean
        val ideResolver: Resolver
        try {
          ideResolver = when (ideToPlugins.key) {
            is IdeDescriptor.ByFile -> {
              closeIdeResolver = true; Resolver.createIdeResolver(ide)
            }
            is IdeDescriptor.ByVersion -> {
              closeIdeResolver = true; Resolver.createIdeResolver(ide)
            }
            is IdeDescriptor.ByInstance -> {
              val ideDescriptor = ideToPlugins.key as IdeDescriptor.ByInstance
              closeIdeResolver = (ideDescriptor.ideResolver == null)
              if (closeIdeResolver) {
                Resolver.createIdeResolver(ide)
              } else {
                ideDescriptor.ideResolver!!
              }
            }
          }
        } catch(ie: InterruptedException) {
          throw ie
        } catch(e: Exception) {
          //IDE errors are propagated.
          throw RuntimeException("Failed to read IDE classes for ${ideToPlugins.key}")
        }

        try {
          ideToPlugins.value.forEach poi@ { pluginOnIde ->
            checkCancelled()

            LOG.info("Verifying ${pluginOnIde.first} against ${pluginOnIde.second}")

            val plugin: Plugin
            try {
              plugin = VParamsCreator.getPlugin(pluginOnIde.first, ide.version)
            } catch(ie: InterruptedException) {
              throw ie
            } catch(e: IncorrectPluginException) {
              //the plugin has incorrect structure.
              val reason = e.message ?: "The plugin ${pluginOnIde.first} has incorrect structure"
              LOG.error(reason, e)
              results.add(VResult.BadPlugin(pluginOnIde.first, pluginOnIde.second, reason))
              return@poi
            } catch(e: UpdateNotFoundException) {
              //the caller has specified a missing plugin
              LOG.error(e.message ?: "The plugin ${pluginOnIde.first} is not found in the Repository", e)
              throw e
            } catch(e: IOException) {
              //the plugin has an invalid file
              val reason = e.message ?: e.javaClass.name
              LOG.error(reason, e)
              results.add(VResult.BadPlugin(pluginOnIde.first, pluginOnIde.second, reason))
              return@poi
            }

            val pluginResolver: Resolver
            try {
              pluginResolver = Resolver.createPluginResolver(plugin)
            } catch(e: Exception) {
              val reason = e.message ?: "Failed to read the class-files of the plugin $plugin"
              LOG.error(reason, e)
              results.add(VResult.BadPlugin(pluginOnIde.first, pluginOnIde.second, reason))
              return@poi
            }

            //auto-close
            pluginResolver.use {
              val ctx = VContext(plugin, pluginResolver, pluginOnIde.first, ide, ideResolver, pluginOnIde.second, runtimeResolver, params.options, params.externalClassPath)

              try {
                val vResult = Verifiers.processAllVerifiers(ctx)
                results.add(vResult)
                LOG.info("Successfully verified $plugin against ${pluginOnIde.second}")
              } catch (ie: InterruptedException) {
                throw ie
              } catch (e: Exception) {
                val message = "Failed to verify ${pluginOnIde.first} against ${pluginOnIde.second}"
                LOG.error(message, e)
                throw RuntimeException(message, e)
              }

            }

          }
        } finally {
          if (closeIdeResolver) {
            ideResolver.close()
          }
        }
      }
    }

    LOG.info("The verification has been successfully completed in ${(System.currentTimeMillis() - time) / 1000} seconds")

    return VResults(results)
  }

  private fun checkCancelled() {
    if (Thread.currentThread().isInterrupted) {
      throw InterruptedException("The verification was cancelled")
    }
  }

}