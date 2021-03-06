package net.samystudio.beaver

import dagger.android.support.DaggerApplication
import io.fabric.sdk.android.Fabric
import net.samystudio.beaver.data.TrimMemory
import net.samystudio.beaver.di.component.DaggerApplicationComponent
import timber.log.Timber
import javax.inject.Inject

class BeaverApplication : DaggerApplication() {
    private val applicationInjector = DaggerApplicationComponent.builder()
        .application(this)
        .build()

    /**
     * @see net.samystudio.beaver.di.module.CrashlyticsModule.provideFabric
     */
    @Inject
    lateinit var fabric: Fabric
    /**
     * @see net.samystudio.beaver.di.module.TimberModule.provideTimberTree
     */
    @Inject
    lateinit var timberTree: Timber.Tree
    /**
     * @see net.samystudio.beaver.di.module.ApplicationModule.provideTrimMemoryList
     */
    @Inject
    lateinit var trimMemoryList: ArrayList<TrimMemory>

    override fun onCreate() {
        super.onCreate()

        Timber.plant(timberTree)

        // Launch screen timeout, this is not material guideline compliant but client is king and
        // most want it displayed longer, just remove if client is material compliant ^^.
        Thread.sleep(1000)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        trimMemoryList.forEach { it.onTrimMemory(level) }
    }

    override fun applicationInjector() = applicationInjector
}