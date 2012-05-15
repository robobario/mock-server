package controllers

import play.api.mvc.Controller
import play.api.libs.json._

class Transformers extends Controller{
  case class Transformer(queryParamSubstitutions : Seq[QuerySubstitutionRule])
  case class QuerySubstitutionRule(token:String, queryParamName:String)

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
      Transformer((json \ "friends").asOpt[List[QuerySubstitutionRule]].getOrElse(List()))
    }

    def writes(transformer: Transformer) = {
      JsObject(Seq("queryParamSubstitutions" -> JsArray(transformer.queryParamSubstitutions.map(q => Json.toJson(q)))))
    }
  }
}
