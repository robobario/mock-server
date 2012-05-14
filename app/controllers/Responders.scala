package controllers

import play.api.data.Form
import play.api.data.Forms._
import controllers.ResponderLogger.{Join, Log}
import play.api.mvc._
import play.api.libs._

import json.Json
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.pattern.ask
import akka.util.Timeout
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files.TemporaryFile
import java.io.File


object Responders extends Controller{
  case class CreateResponder(body: String, headers : List[Header])
  case class Header(name:String, value:String)

  val form:Form[CreateResponder] = Form(
    mapping(
      "body" -> text,
      "headers" -> list(mapping(
        "name"-> text,
        "value"-> text
      )(Header.apply)(Header.unapply))
    )(CreateResponder.apply)(CreateResponder.unapply)
  )

  def toResponderForm(responder: Responder): Responders.CreateResponder = {
     CreateResponder(responder.body,responder.headers.map(h=>Header(h._1,h._2)).toList)
  }

  def show(responderName:String) = Action{
    Async{ResponderLibrary.apply(responderName).map(responder=>
      detailsOrCreationForm(responder, responderName)
    )}
  }

  def edit(responderName:String) = Action{
    Async{ResponderLibrary(responderName).map(responder=>
      responder.map(r=>Ok(views.html.newresponder(form.fill(toResponderForm(r)),responderName))).getOrElse(Ok(views.html.newresponder(form,responderName)))
    )}
  }

  def listResponders = Action{
    implicit request =>
    Async{ResponderLibrary.all.map(responders=>
      Ok(views.html.index(responders.map(responder => responder.name -> routes.Responders.show(responder.name).absoluteURL(false))))
    )}
  }


  def detailsOrCreationForm(responder: Option[Responder],responderName:String): Result = {
    responder.map(r=>Ok(views.html.existingresponder(r))).getOrElse(Ok(views.html.newresponder(form,responderName)))
  }

  def renderResponder(responderName:String) = Action{
    request =>
    Async{ResponderLibrary(responderName).map(responder=>
      responder.map(responder => createResponse(responder,request)).getOrElse(NotFound)
    )}
  }


  def createResponse(responder: Responder,request:Request[_]): Result = {
    Actors.loggers ! Log(responder.name, request)
    responder.absolutePath.map(path=>binaryResponse(path,responder)).getOrElse(Ok(responder.body).withHeaders(responder.headers: _*))
  }


  def binaryResponse(path:String,responder:Responder): Result = {
    Ok.stream(Enumerator.fromFile(new File(path))).withHeaders(responder.headers:_*)
  }

  def handleSubmission(responderName:String, responder: CreateResponder, binary:Option[FilePart[TemporaryFile]]): Result = {
    Async{
      ResponderLibrary.create(responderName, responder.body,binary,responder.headers.map(header=>header.name -> header.value):_*).map(f=>
          Ok(views.html.existingresponder(f))
      )
    }
  }

  def submit(responderName:String) = Action(parse.multipartFormData) {
    implicit request=>
      val responder: CreateResponder = form.bindFromRequest.get
      handleSubmission(responderName,responder,request.body.file("binary"))
  }

  //Called from existingresponder.scala.html
  def stream(name:String) = Action {
    AsyncResult {
      implicit val timeout:Timeout = 5000
      (Actors.loggers ? (Join(name)) ).mapTo[Enumerator[LoggedRequest]].asPromise.map { chunks =>
        Ok.stream(chunks &> Comet[LoggedRequest](callback = "parent.message")(encoder = Comet.CometMessage[LoggedRequest](request => Json.toJson(request).toString())))
      }
    }
  }
}
