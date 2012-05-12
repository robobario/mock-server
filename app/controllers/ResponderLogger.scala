package controllers

import akka.actor.Actor
import play.api.libs.iteratee.{Enumerator, PushEnumerator}
import controllers.ResponderLogger.{Quit, Join, Log}

class ResponderLogger extends Actor{

  var responderToLogPushers:Map[String,Seq[PushEnumerator[String]]] = Map()

  def doLog(name: String, message: String) {
    responderToLogPushers.get(name).map(_.foreach(_.push(message)))
  }

  def doJoin(name: String) {
    lazy val channel: PushEnumerator[String] =  Enumerator.imperative[String](onComplete = self ! Quit(name,channel))
    val pushers = responderToLogPushers.get(name).map(f=>f :+ channel).getOrElse(Seq(channel))
    responderToLogPushers = responderToLogPushers + (name -> pushers)
    sender ! channel
  }

  def doQuit(name: String, channel: PushEnumerator[String]) {
    val pushers = responderToLogPushers.get(name).map(f=>f.filterNot(_ == channel)).getOrElse(Seq())
    responderToLogPushers = responderToLogPushers + (name -> pushers)
  }

  def receive = {
    case Log(name, message) => doLog(name,message)
    case Join(name) => doJoin(name)
    case Quit(name,pusher) => doQuit(name,pusher)
  }
}

object ResponderLogger {
  trait Event
  case class Log(name: String, message:String) extends Event
  case class Join(name: String) extends Event
  case class Quit(name: String,channel: PushEnumerator[String]) extends Event
}