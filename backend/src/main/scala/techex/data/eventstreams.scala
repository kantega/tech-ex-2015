package techex.data

import techex.domain.Fact

import scalaz.stream.async
import scalaz.stream.async.mutable.Topic

object eventstreams {


  lazy val events: Topic[InputMessage] =
    scalaz.stream.async.topic()

  val factUdpates: Topic[Fact] =
    async.topic()

}

trait InputMessage
trait Command extends InputMessage