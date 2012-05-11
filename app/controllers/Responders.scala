package controllers

import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

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
    Ok(views.html.newresponder(form,responderName))
  }

  def renderResponder(responderName:String) = Action{
    Async{ResponderLibrary.apply(responderName)}
  }

  def handleSubmission(responderName:String, responder: CreateResponder): Result = {
    ResponderLibrary.create(responderName, responder.body, responder.headers.map(header=>header.name -> header.value):_*)
    Ok("woot")
  }

  def submit(responderName:String) = Action {
    implicit request=>
      val responder: CreateResponder = form.bindFromRequest.get
      handleSubmission(responderName,responder)
  }
}
