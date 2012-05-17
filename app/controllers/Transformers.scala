package controllers

import play.api.libs.json._
import akka.pattern.ask
import play.api.libs.concurrent._
import play.api.mvc.{Result, Action, Controller}
import akka.util.Timeout

case class Transformer(queryParamSubstitutions : Seq[SubstitutionRule],headerSubstitutions : Seq[SubstitutionRule], cookieSubstitutions : Seq[SubstitutionRule])
case class SubstitutionRule(token:String, paramName:String)

object Transformers extends Controller{
  implicit val timeout:Timeout = 5000

  implicit object QuerySubstitutionRuleFormat extends Format[SubstitutionRule] {
    def reads(json: JsValue) = {
      SubstitutionRule(
        (json \ "token").as[String],
        (json \ "paramName").as[String]
      )
    }

    def writes(transformer: SubstitutionRule) = {
        JsObject(  Seq(
          "token" -> JsString(transformer.token),
          "paramName" -> JsString(transformer.paramName)
        )
      )
    }
  }

  implicit object TransformerFormat extends Format[Transformer] {
    def reads(json: JsValue) = {
      Transformer((json \ "queryParamSubstitutions").asOpt[List[SubstitutionRule]].getOrElse(List()),
        (json \ "headerSubstitutions").asOpt[List[SubstitutionRule]].getOrElse(List()),
        (json \ "cookieSubstitutions").asOpt[List[SubstitutionRule]].getOrElse(List())
      )
    }

    def writes(transformer: Transformer) = {
      JsObject(Seq("queryParamSubstitutions" -> JsArray(transformer.queryParamSubstitutions.map(q => Json.toJson(q))),
                   "headerSubstitutions" -> JsArray(transformer.headerSubstitutions.map(q => Json.toJson(q))),
                    "cookieSubstitutions" -> JsArray(transformer.cookieSubstitutions.map(q => Json.toJson(q)))
                   )
      )
    }
  }

  def putTransformer(responderName:String) = Action(parse.json){
    def responseFromTransformer: (Any) => Result = {
      case x:Boolean if x => Ok
      case _ => InternalServerError
    }
    request =>
      request.body.asOpt[Transformer].map{ t=>
        Async(
          (Actors.transformers ? PutTransformer(responderName,t)).asPromise.map(responseFromTransformer)
        )
      }.getOrElse(InternalServerError)
  }

  def getTransformer(responderName:String) = Action {
    Async(
      (Actors.transformers ? GetTransformer(responderName)).map{
        case Some(x:Transformer) => Ok(views.html.transformer(Json.stringify(Json.toJson(x)),responderName))
        case _ => Ok(views.html.transformer("{}",responderName))
      }.asPromise
    )
  }
}
