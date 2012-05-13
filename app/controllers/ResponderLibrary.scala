package controllers

import akka.actor.Actor
import akka.pattern.ask
import play.api.libs.concurrent._
import akka.util.Timeout._
import akka.util.Timeout
import controllers.ResponderLibrary.{GetAll, CreateResponder, GetResponder}
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import java.io.File
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import play.api.Play.current
import scala.Predef._

case class Responder(name:String, body:String, headers:Seq[(String,String)],binary:Option[File])

object ResponderLibrary {
  trait Event

  val documentBase = current.configuration.getString("document.base").getOrElse("/tmp")
  case class GetResponder(name: String) extends Event
  case class CreateResponder(name:String, body:String, headers:Seq[(String,String)],binary:Option[FilePart[TemporaryFile]]) extends Event
  case class GetAll() extends Event
  implicit val timeout:Timeout = 5000

  def apply(name:String) = {
    (Actors.responders ? GetResponder(name)).asPromise.map(f => f.asInstanceOf[Option[Responder]])
  }

  def create(name:String, body:String,binary:Option[FilePart[TemporaryFile]], headers:(String,String)*) = {
    (Actors.responders ? CreateResponder(name,body,headers,binary)).asPromise.map(f => f.asInstanceOf[Responder])
  }

  def all = {
    (Actors.responders ? GetAll()).asPromise.map(f => f.asInstanceOf[Iterable[Responder]])
  }
}

class ResponderLibrary extends Actor {
  var responders:Map[String,Responder] = Map();
  def receive = {
    case GetResponder(name) => sender ! responders.get(name)
    case CreateResponder(name, body, headers,binary) => create(name, body, headers,binary)
    case GetAll() => sender ! responders.values
  }

  def create(name: String, body: String, headers:Seq[(String,String)],binary:Option[FilePart[TemporaryFile]]) {
    val heads = binary.map(f=>headers :+ ("Content-Length"->f.ref.file.length.toString)).getOrElse(headers)
    val responder: Responder = Responder(name, body, heads, binary.map(f=>moveFile(f,name)))
    responders = responders + (name -> responder)
    sender ! responder
  }

  def moveFile(f: MultipartFormData.FilePart[Files.TemporaryFile],responderName:String): File = {
    val newFile: File = new File(ResponderLibrary.documentBase+"/"+responderName+"/"+f.filename)
    f.ref.moveTo(newFile,true)
    newFile
  }
}
