package techex.data

import scalaz.stream.async.mutable.Topic

object eventstreams {


  lazy val events: Topic[StreamEvent] =
    scalaz.stream.async.topic()


}

trait StreamEvent
trait Command extends StreamEvent