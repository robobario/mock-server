package controllers

import play.api.libs.json._
import akka.pattern.ask
import play.api.libs.concurrent._
import play.api.mvc.{Result, Action, Controller}
import akka.util.Timeout

case class Transformer(queryParamSubstitutions : Seq[QuerySubstitutionRule])
case class QuerySubstitutionRule(token:String, queryParamName:String)

object Transformers extends Controller{
  implicit val timeout:Timeout = 5000

  implicit object QuerySubstitutionRuleFormat extends Format[QuerySubstitutionRule] {
    def reads(json: JsValue) = {
      QuerySubstitutionRule(
        (json \ "token").as[String],
        (json \ "queryParamName").as[String]
      )
    }

    def writes(transformer: QuerySubstitutionRule) = {
        JsObject(  Seq(
          "token" -> JsString(transformer.token),
          "queryParamName" -> JsString(transformer.queryParamName)
        )
      )
    }
  }

  implicit object TransformerFormat extends Format[Transformer] {
    def reads(json: JsValue) = {
      Transformer((json \ "queryParamSubstitutions").asOpt[List[QuerySubstitutionRule]].getOrElse(List()))
    }

    def writes(transformer: Transformer) = {
      JsObject(Seq("queryParamSubstitutions" -> JsArray(transformer.queryParamSubstitutions.map(q => Json.toJson(q)))))
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
        case Some(x:Transformer) => Ok(Json.toJson(x))
        case _ => NotFound
      }.asPromise
    )
  }
}
