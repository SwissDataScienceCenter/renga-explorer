package controllers

import javax.inject.{ Inject, Singleton }

import ch.datascience.service.swagger.{ SwaggerControllerHelper, YamlHelper }
import play.api.Configuration
import play.api.libs.json.JsObject
import play.api.mvc._

@Singleton
class SwaggerController @Inject() ( val config: Configuration ) extends Controller with SwaggerControllerHelper {

  def swaggerSpec: JsObject = _swaggerSpec

  private[this] lazy val _swaggerSpec: JsObject = loadSwaggerSpec

  private[this] def loadSwaggerSpec = {
    val is = getClass.getResourceAsStream( "/swagger.yml" )
    YamlHelper.convertYamlToJson( is ).as[JsObject]
  }

}
