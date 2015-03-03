package techex

import org.specs2.mutable._
import techex.data.googleNotifications
import techex.domain.DeviceToken

class SendMsgToZsuzsana extends Specification {

  "Sending messages to zsuzor" should {

    "succeed" in {
      val task = googleNotifications.sendMessage(
        DeviceToken("APA91bFhUhtiTN7VPI0gMRbxaT3b6BzjtCkDemSFTV0kR0l1iD1xzwyG19q1F-aV5oCNzdYnruy7qVEULnXMTXHNrMV3WTFmhIcsM438YhLEFSour2Npcxg9l0Q8PDvbJNU3e30fcFf5YHVSO6PtGs0ZqEfLDDwIy8Vyu8U6QOchzHh4QI-tSLs"),
        "Test"
      )

      task.attemptRun.isLeft must_== false
    }

  }

}
