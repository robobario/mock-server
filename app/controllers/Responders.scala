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
import play.api.Play.current
import play.api.Logger

object Responders extends Controller{
  case class CreateResponder(body: String, headers : List[Header], responseCode : scala.Int)
  case class CreateFileResponder(headers : List[Header])
  case class Header(name:String, value:String)
  implicit val timeout:Timeout = 5000

  val form:Form[CreateResponder] = Form(
    mapping(
      "body" -> text,
      "headers" -> list(mapping(
        "name"-> text,
        "value"-> text
      )(Header.apply)(Header.unapply)),
      "responseCode" -> number
    )(CreateResponder.apply)(CreateResponder.unapply)
  )

  val fileForm:Form[CreateFileResponder] = Form(
    mapping(
      "headers" -> list(mapping(
        "name"-> text,
        "value"-> text
      )(Header.apply)(Header.unapply))
    )(CreateFileResponder.apply)(CreateFileResponder.unapply)
  )

  def toResponderForm(responder: Responder): Responders.CreateResponder = {
     CreateResponder(responder.body,responder.headers.map(h=>Header(h._1,h._2)).toList,responder.responseCode)
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
    Async{ResponderLibrary(responderName).flatMap(responder=>
      responder.map(responder => applyTransformers(responder, request)).getOrElse(Akka.future(NotFound))
    )}
  }


  def applyTransformers(responder: Responder, request: Request[AnyContent]): Promise[Result] = {
    createResponse(responder, request)
  }

  def createResponse(responder: Responder,request:Request[_]): Promise[Result] = {
    Actors.loggers ! Log(responder.name, request)
    responder.absolutePath.map(path=>Akka.future(binaryResponse(path,responder))).getOrElse(createBodyResponse(responder,request))
  }


  def createBodyResponse(responder: Responder,request:Request[_]): Promise[SimpleResult[String]] = {
    def doQueryParamSubstitution(): (String, SubstitutionRule) => String = {
      (body: String, rule: SubstitutionRule) =>
        request.queryString.get(rule.paramName).map(_.mkString).map{param=>
          body.replaceAll(rule.token, param)
        }.getOrElse(body)
    }

    def doHeaderSubstitution(): (String, SubstitutionRule) => String = {
      (body: String, rule: SubstitutionRule) =>
        request.headers.get(rule.paramName).map{param=>
          body.replaceAll(rule.token, param)
        }.getOrElse(body)
    }

    def doCookieSubstitution(): (String, SubstitutionRule) => String = {
      (body: String, rule: SubstitutionRule) =>
        request.cookies.get(rule.paramName).map{param=>
          body.replaceAll(rule.token, param.value)
        }.getOrElse(body)
    }

    def applyRulesToBody(queryRules: scala.Seq[SubstitutionRule],headerRules: scala.Seq[SubstitutionRule],cookieRules: scala.Seq[SubstitutionRule]): String = {
      val querySubbed = queryRules.foldLeft[String](responder.body) {
        doQueryParamSubstitution()
      }
      val headerSubbed = headerRules.foldLeft[String](querySubbed) {
        doHeaderSubstitution()
      }
      cookieRules.foldLeft[String](headerSubbed) {
        doCookieSubstitution()
      }
    }
    (Actors.transformers ? GetTransformer(responder.name)).map{
      case Some(Transformer(queryParam,headerParam,cookieRules))=> applyRulesToBody(queryParam,headerParam,cookieRules)
      case _ => responder.body
    }.map(body => Status(responder.responseCode)(body).withHeaders(responder.headers: _*)).asPromise
  }

  def binaryResponse(path:String,responder:Responder): Result = {
    Ok.stream(Enumerator.fromFile(new File(path))).withHeaders(responder.headers:_*)
  }

  def handleSubmission(responderName:String, responder: CreateResponder, binary:Option[FilePart[TemporaryFile]]): Result = {
    Async{
      ResponderLibrary.create(responderName, responder.body,responder.responseCode,binary,responder.headers.map(header=>header.name -> header.value):_*).map(f=>
          Ok(views.html.existingresponder(f))
      )
    }
  }

  def handleFileSubmission(responderName:String, responder: CreateFileResponder, binary:Option[FilePart[TemporaryFile]]): Result = {
    Async{
      ResponderLibrary.create(responderName, "" ,200,binary,responder.headers.map(header=>header.name -> header.value):_*).map(f=>
        Ok(views.html.existingresponder(f))
      )
    }
  }

  def submitFile(responderName:String) = Action(parse.multipartFormData) {
    implicit request=>
      val responder: CreateFileResponder = fileForm.bindFromRequest.get
      handleFileSubmission(responderName,responder,request.body.file("binary"))
  }

  def submit(responderName:String) = Action {
    implicit request=>
      val responder: CreateResponder = form.bindFromRequest.get
      handleSubmission(responderName,responder, None)
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
