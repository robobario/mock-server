package controllers

import akka.actor.Actor
import akka.pattern.ask
import play.api.libs.concurrent._
import akka.util.Timeout._
import akka.util.Timeout
import controllers.ResponderLibrary.{GetAll, CreateResponder, GetResponder}
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import play.api.Play.current
import scala.Predef._
import play.api.libs.json._
import java.io.{FilenameFilter, File}

case class Responder(name: String, body: String, headers: Seq[(String, String)], absolutePath: Option[String]) {

  def absoluteUrl: String = {
    routes.Responders.renderResponder(name).url
  }
}

object Responder {

  implicit object ResponderFormat extends Format[Responder] {

    def reads(json: JsValue): Responder = Responder((json \ "name").as[String], (json \ "body").as[String],
      (json \ "headers").as[Seq[JsValue]].map(js => (js \ "name").as[String] -> (js \ "value").as[String]),
      (json \ "url").asOpt[String])


    def writes(r: Responder): JsValue = {
      val list: List[(String, JsValue with Product with Serializable)] = List("name" -> JsString(r.name),
        "body" -> JsString(r.body), "headers" ->
          JsArray(r.headers.map(hd => JsObject(List("name" -> JsString(hd._1), "value" -> JsString(hd._2))))))
      JsObject(r.absolutePath.map(filePath => list.::("url" -> JsString(filePath))).getOrElse(list))
    }
  }

}

object ResponderLibrary {

  trait Event

  val documentBase = current.configuration.getString("document.base").getOrElse("/tmp/mockserve")

  case class GetResponder(name: String) extends Event

  case class CreateResponder(name: String, body: String, headers: Seq[(String, String)],
                             binary: Option[FilePart[TemporaryFile]]) extends Event

  case class GetAll() extends Event

  implicit val timeout: Timeout = 5000


  def apply(name: String) = {
    (Actors.responders ? GetResponder(name)).asPromise.map(f => f.asInstanceOf[Option[Responder]])
  }


  def create(name: String, body: String, binary: Option[FilePart[TemporaryFile]], headers: (String, String)*) = {
    (Actors.responders ? CreateResponder(name, body, headers, binary)).asPromise.map(f => f.asInstanceOf[Responder])
  }


  def all = {
    (Actors.responders ? GetAll()).asPromise.map(f => f.asInstanceOf[Iterable[Responder]])
  }

}

class ResponderLibrary extends Actor {

  var responders: Map[String, Responder] = loadInitial


  def loadInitial: Map[String, Responder] = {
    val root: java.io.File = new File(ResponderLibrary.documentBase)
    if (root.exists()) {
      root.listFiles().filter(_.isDirectory).filter(parentDir => !parentDir.list().isEmpty).filter(parentDir => !parentDir.list(ExactMatchFilenameFilter.forName("responder.info")).isEmpty)
        .map(parent => parent.getName -> loadResponder(parent.listFiles(ExactMatchFilenameFilter.forName("responder.info"))(0))).toMap
    }
    else {
      Map()
    }
  }

  def loadResponder(file:File): Responder = {
     Json.parse(scala.io.Source.fromFile(file).mkString).as[Responder]
  }



  def receive = {
    case GetResponder(name) => sender ! responders.get(name)
    case CreateResponder(name, body, headers, binary) => create(name, body, headers, binary)
    case GetAll() => sender ! responders.values
  }


  def create(name: String, body: String, headers: Seq[(String, String)], binary: Option[FilePart[TemporaryFile]]) {
    val heads = binary.map(f => headers :+ ("Content-Length" -> f.ref.file.length.toString)).getOrElse(headers)
    val responder: Responder = Responder(name, body, heads, binary.map(f => moveFile(f, name).getAbsolutePath))
    val f = new File(ResponderLibrary.documentBase + "/" + name)
    if (!f.exists()) {
      f.mkdirs()
    }
    printToFile(new File(ResponderLibrary.documentBase + "/" + name + "/responder.info")) {
      p => p.print(Json.stringify(Json.toJson(responder)))
    }
    responders = responders + (name -> responder)
    sender ! responder
  }


  def moveFile(f: MultipartFormData.FilePart[Files.TemporaryFile], responderName: String): File = {
    val newFile: File = new File(ResponderLibrary.documentBase + "/" + responderName + "/" + f.filename)
    f.ref.moveTo(newFile, replace = true)
    newFile
  }


  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    if (!f.exists()) {
      f.createNewFile()
    }
    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    }
    finally {
      p.close()
    }
  }
}

object ExactMatchFilenameFilter{
  def forName(name:String):FilenameFilter = {
    new FilenameFilter {
      def accept(file: File, filename: String): Boolean = {filename.equals(name)}
    }
  }
}