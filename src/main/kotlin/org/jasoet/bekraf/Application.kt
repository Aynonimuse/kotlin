package org.jasoet.bekraf

import com.hazelcast.config.Config
import io.vertx.config.ConfigRetriever
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.logging.SLF4JLogDelegateFactory
import io.vertx.core.spi.cluster.ClusterManager
import io.vertx.kotlin.config.ConfigRetrieverOptions
import io.vertx.kotlin.config.ConfigStoreOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
import org.jasoet.bekraf.extension.env
import org.jasoet.bekraf.extension.logger
import org.jasoet.bekraf.extension.single
import org.jasoet.bekraf.module.DaggerAppComponent
import org.jasoet.bekraf.module.MongoModule
import org.jasoet.bekraf.module.VertxModule
import org.jasoet.bekraf.verticle.MainVerticle
import java.net.InetAddress


/**
 * [Documentation Here]
 *
 * @author Sandiah Ahsan, Deny Prasetyo
 */

class Application {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory::class.java.name)
            val log = logger(Application::class)

            val hazelcastConfig = Config()

            val hazelManager: ClusterManager = HazelcastClusterManager(hazelcastConfig)

            val vertxOption = VertxOptions().apply {
                this.clusterManager = hazelManager
                try {
                    val address = InetAddress.getByName(env("HOSTNAME", "localhost")).hostAddress
                    this.clusterHost = address
                    log.info("Cluster set to use clusterHost ${this.clusterHost}")
                } catch (e: Exception) {
                    log.info("Hostname Berdaya not Found, perhaps you run this app locally!")
                }
            }

            single<Vertx> { Vertx.clusteredVertx(vertxOption, it) }
                    .flatMap {
                        val vertx = it
                        val json = ConfigStoreOptions(
                                type = "file",
                                format = "json",
                                config = json {
                                    obj("path" to "application-config.json")
                                }
                        )
                        val env = ConfigStoreOptions(
                                type = "env")
                        val options = ConfigRetrieverOptions(
                                stores = listOf(json, env)
                        )

                        val retriever = ConfigRetriever.create(vertx, options)
                        single<JsonObject> { retriever.getConfig(it) }
                                .map { it to vertx }
                    }
                    .doOnError {
                        log.error("Error Occurred when deploying/retrieving config ${it.message}", it)
                    }
                    .subscribe {
                        val (config, vertx) = it

                        log.info("Initialize Components...")

                        val app = DaggerAppComponent.builder()
                                .vertxModule(VertxModule(vertx, config))
                                .mongoModule(MongoModule(config))
                                .build()

                        val initializer = app.initializer()
                        initializer()

                        vertx.deployVerticle(MainVerticle(app.mainController()), DeploymentOptions().apply {
                            this.config = config
                        })

                    }
        }
    }
}
