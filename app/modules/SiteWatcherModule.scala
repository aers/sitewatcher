package modules

import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.{SiteWatcherScheduler, SiteWatcher}

class SiteWatcherModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[SiteWatcher]("site-watcher")
    bind(classOf[SiteWatcherScheduler]).asEagerSingleton()
  }
}