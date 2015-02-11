package techex.data


import scalaz._, Scalaz._
import scalaz.concurrent.Task
import scalaz.effect.{ SafeApp, IO }

import doobie.imports._
import doobie.imports.ConnectionIO
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import techex.domain.{PlayerId, Player}


import java.sql.Connection

// HikariCP's connection pool wrapped in an effectful target monad. Note that construction isn't RT so
// we make the smart constructor an effect as well. In real life we would probably add operations
// to inspect and configure the pool.

final class HikariConnectionPoolTransactor[M[_] : Monad : Catchable : Capture] private(pool: HikariDataSource) extends Transactor[M] {
  protected def connect: M[Connection] =
    Capture[M].apply(pool.getConnection)
}

object HikariConnectionPoolTransactor {
  def create[M[_] : Monad : Catchable : Capture](config: HikariConfig): M[HikariConnectionPoolTransactor[M]] =
    Capture[M].apply(new HikariConnectionPoolTransactor(new HikariDataSource(config)))
}


object db {

  def mysqlConfig(username:String,password:String) = {
    val config = new HikariConfig()
    config.setUsername(username)
    config.setPassword(password)
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    config.addDataSourceProperty("useServerPrepStmts", "true")
    config.setDriverClassName("org.mysql.Driver")
    config.setJdbcUrl("jdbc:mysql://mysql.kantega.no/technoport_experiments_2015")
    config
  }

  def inMemConfig = {
    val config = new HikariConfig()
    config.setUsername("sa")
    config.setPassword("")
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", "250")
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
    config.addDataSourceProperty("useServerPrepStmts", "true")
    config.setDriverClassName("org.h2.Driver") //"org.mysql.Driver")
    config.setJdbcUrl("jdbc:h2:mem:test") //"jdbc:mysql://mysql.kantega.no/technoport_experiments_2015")
    config
  }
  def ds(config:HikariConfig) =
    HikariConnectionPoolTransactor.create[Task](config)

}

