package techex.data

import scalaz.stream.async.mutable.Topic

object eventstreams {


  lazy val events: Topic[InputMessage] =
    scalaz.stream.async.topic()

}

trait InputMessage
trait Command extends InputMessage