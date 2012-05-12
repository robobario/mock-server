package controllers

import akka.actor.{Props, ActorSystem}


object Actors {
  lazy val system = ActorSystem("responderLib");
  lazy val responders = system.actorOf(Props[ResponderLibrary])
  lazy val loggers = system.actorOf(Props[ResponderLogger])
}
