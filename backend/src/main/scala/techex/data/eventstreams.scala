package techex.data

import techex.domain.Observation

import scalaz.stream.async.mutable.Topic

object eventstreams {


  lazy val observations:Topic[Observation] =
    scalaz.stream.async.topic()




}
