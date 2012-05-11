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

object ResponderLibrary {
  trait Event
  case class GetResponder(name: String) extends Event
  case class CreateResponder(name:String, body:String, headers:Seq[(String,String)]) extends Event
  implicit val timeout:Timeout = 5000

  lazy val system = ActorSystem("responderLib");
  lazy val ref = system.actorOf(Props[ResponderLibrary])

  def apply(name:String) = {
    (ref ? GetResponder(name)).asPromise.map(f => f.asInstanceOf[Result])
  }

  def create(name:String, body:String, headers:(String,String)*) = {
    (ref ? CreateResponder(name,body,headers)).asPromise.map(f => f.asInstanceOf[Result])
  }
}

class ResponderLibrary extends Actor {
  var responders:Map[String,Result] = Map();
  def receive = {
    case GetResponder(name) => sender ! responders(name)
    case CreateResponder(name, body, headers) => create(name, body, headers)
  }

  def create(name: String, body: String, headers:Seq[(String,String)]) {
    responders = responders + (name -> Ok(body).withHeaders(headers:_*))
    sender ! Ok("woot")
  }
}
