package controllers

import play.api.mvc._
import views.html.helper.FieldConstructor

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }
  
}

object MyHelpers {

  implicit val myFields = FieldConstructor(views.html.bootstraphelper.f)

}