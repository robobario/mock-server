package controllers

import akka.actor.{Props, ActorSystem, Actor}
import akka.pattern.ask
import play.api.libs.concurrent._
import controllers.ResponderLibrary.{CreateResponder, GetResponder}
import play.api.mvc.Results._
import play.api.mvc._
import akka.util.Timeout._
import akka.util.Timeout
import scala.Predef._

case class Responder(name:String, body:String, headers:Seq[(String,String)])

object ResponderLibrary {
  trait Event
  case class GetResponder(name: String) extends Event
  case class CreateResponder(name:String, body:String, headers:Seq[(String,String)]) extends Event
  implicit val timeout:Timeout = 5000

  def apply(name:String) = {
    (Actors.responders ? GetResponder(name)).asPromise.map(f => f.asInstanceOf[Option[Responder]])
  }

  def create(name:String, body:String, headers:(String,String)*) = {
    (Actors.responders ? CreateResponder(name,body,headers)).asPromise.map(f => f.asInstanceOf[Responder])
  }
}

class ResponderLibrary extends Actor {
  var responders:Map[String,Responder] = Map();
  def receive = {
    case GetResponder(name) => sender ! responders.get(name)
    case CreateResponder(name, body, headers) => create(name, body, headers)
  }

  def create(name: String, body: String, headers:Seq[(String,String)]) {
    val responder: Responder = Responder(name, body, headers)
    responders = responders + (name -> responder)
    sender ! responder
  }
}
