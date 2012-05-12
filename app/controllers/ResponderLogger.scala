package controllers

import akka.actor.Actor
import play.api.libs.iteratee.{Enumerator, PushEnumerator}
import controllers.ResponderLogger.{Quit, Join, Log}
import play.api.mvc.Request
import java.util.Date
import java.text.SimpleDateFormat

class ResponderLogger extends Actor {

  var responderToLogPushers: Map[String, Seq[PushEnumerator[String]]] = Map()

  def doLog(name: String, request: Request[_]) {
    responderToLogPushers.get(name).map(_.foreach(_.push(transformToLogMessage(request))))
  }


  def transformToLogMessage(request: Request[_]): String = {
    val base: String = (new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z") format (new Date(): Date)) + ": " + request.toString
    base + request.contentType.map(" Content-Type: " + _).getOrElse("") + request.headers.get("Accept").map(" Accept: " + _).getOrElse("")
  }

  def doJoin(name: String) {
    lazy val channel: PushEnumerator[String] = Enumerator.imperative[String](onComplete = self ! Quit(name, channel))
    val pushers = responderToLogPushers.get(name).map(f => f :+ channel).getOrElse(Seq(channel))
    responderToLogPushers = responderToLogPushers + (name -> pushers)
    sender ! channel
  }

  def doQuit(name: String, channel: PushEnumerator[String]) {
    val pushers = responderToLogPushers.get(name).map(f => f.filterNot(_ == channel)).getOrElse(Seq())
    responderToLogPushers = responderToLogPushers + (name -> pushers)
  }

  def receive = {
    case Log(name, message) => doLog(name, message)
    case Join(name) => doJoin(name)
    case Quit(name, pusher) => doQuit(name, pusher)
  }
}

object ResponderLogger {

  trait Event

  case class Log(name: String, request: Request[_]) extends Event

  case class Join(name: String) extends Event

  case class Quit(name: String, channel: PushEnumerator[String]) extends Event

}