package org.jasoet.bekraf.module

import com.mongodb.client.MongoDatabase
import dagger.Component
import org.jasoet.bekraf.controller.MainController
import org.mongodb.morphia.Datastore
import javax.inject.Named
import javax.inject.Singleton

/**
 * [Documentation Here]
 *
 * @author Deny Prasetyo
 */


@Singleton
@Component(modules = arrayOf(VertxModule::class, MongoModule::class))
interface AppComponent {
    fun database(): MongoDatabase
    fun dataStore(): Datastore
    fun mainController(): MainController
    @Named("dataInitializer") fun initializer(): () -> Unit
}