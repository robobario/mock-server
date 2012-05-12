package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import controllers.ResponderLogger.Log

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

  def show(responderName:String) = Action{
    Async{ResponderLibrary.apply(responderName).map(responder=>
      detailsOrCreationForm(responder, responderName)
    )}
  }


  def detailsOrCreationForm(responder: Option[Responder],responderName:String): Result = {
    responder.map(r=>Ok(views.html.existingresponder(r))).getOrElse(Ok(views.html.newresponder(form,responderName)))
  }

  def renderResponder(responderName:String) = Action{
    request =>
    Async{ResponderLibrary.apply(responderName).map(responder=>
      responder.map(responder => createResponse(responder,request)).getOrElse(NotFound)
    )}
  }


  def createResponse(responder: Responder,request:Request[_]): SimpleResult[String] = {
    Actors.loggers ! Log(responder.name, request.toString)
    Ok(responder.body).withHeaders(responder.headers: _*)
  }

  def handleSubmission(responderName:String, responder: CreateResponder): Result = {
    Async{
      ResponderLibrary.create(responderName, responder.body, responder.headers.map(header=>header.name -> header.value):_*).map(f=>
          Ok(views.html.existingresponder(f))
      )
    }
  }

  def submit(responderName:String) = Action {
    implicit request=>
      val responder: CreateResponder = form.bindFromRequest.get
      handleSubmission(responderName,responder)
  }
}
