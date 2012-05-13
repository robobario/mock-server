package controllers

import play.api.mvc._
import views.html.helper.FieldConstructor

object MyHelpers {

  implicit val myFields = FieldConstructor(views.html.bootstraphelper.f)

}