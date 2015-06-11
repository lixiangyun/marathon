package mesosphere.marathon.api

import javax.inject.Named
import javax.net.ssl.SSLContext

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.google.inject.servlet.ServletModule
import com.google.inject.{ Singleton, Provides, Scopes }
import mesosphere.chaos.http.{ HttpConf, RestModule }
import mesosphere.jackson.CaseClassModule
import mesosphere.marathon.api.v2.json.MarathonModule
import mesosphere.marathon.event.http.HttpEventStreamServlet
import mesosphere.marathon.io.SSLContextUtil

/**
  * Setup the dependencies for the LeaderProxyFilter.
  * This filter will redirect to the master if running in HA mode.
  */
class LeaderProxyFilterModule extends ServletModule {
  protected override def configureServlets() {
    bind(classOf[RequestForwarder]).to(classOf[JavaUrlConnectionRequestForwarder]).in(Scopes.SINGLETON)
    bind(classOf[LeaderProxyFilter]).asEagerSingleton()
    filter("/*").through(classOf[LeaderProxyFilter])
  }

  /**
    * Configure ssl using the key store so that our own certificate is accepted
    * in any case, even if it is not signed by a public certification entity.
    */
  @Provides
  @Singleton
  @Named(JavaUrlConnectionRequestForwarder.NAMED_LEADER_PROXY_SSL_CONTEXT)
  def provideSSLContext(httpConf: HttpConf): SSLContext = {
    SSLContextUtil.createSSLContext(httpConf.sslKeystorePath.get, httpConf.sslKeystorePassword.get)
  }
}

class MarathonRestModule extends RestModule {

  override val jacksonModules = Seq(
    new DefaultScalaModule with CaseClassModule,
    new MarathonModule
  )

  protected override def configureServlets() {
    // Map some exceptions to HTTP responses
    bind(classOf[MarathonExceptionMapper]).asEagerSingleton()

    // V2 API
    bind(classOf[v2.AppsResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.TasksResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.EventSubscriptionsResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.QueueResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.GroupsResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.InfoResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.LeaderResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.DeploymentsResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.ArtifactsResource]).in(Scopes.SINGLETON)
    bind(classOf[v2.SchemaResource]).in(Scopes.SINGLETON)

    install(new LeaderProxyFilterModule)

    bind(classOf[CORSFilter]).asEagerSingleton()
    filter("/*").through(classOf[CORSFilter])

    bind(classOf[CacheDisablingFilter]).asEagerSingleton()
    filter("/*").through(classOf[CacheDisablingFilter])

    bind(classOf[HttpEventStreamServlet]).asEagerSingleton()
    serve("/v2/events").`with`(classOf[HttpEventStreamServlet])

    super.configureServlets()
  }
}
