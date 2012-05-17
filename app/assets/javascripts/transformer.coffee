class Transformer
  self = null
  constructor: (@transformerJson, @responderName) ->
      self = this
  putQueryParamRule: (token, queryParamName) ->
      if !(@transformerJson.queryParamSubstitutions?)
          @transformerJson.queryParamSubstitutions = []
      @transformerJson.queryParamSubstitutions = [{token : token, paramName: queryParamName}]
  putHeaderParamRule: (token, headerParamName) ->
    if !(@transformerJson.headerSubstitutions?)
      @transformerJson.headerSubstitutions = []
    @transformerJson.headerSubstitutions = [{token : token, paramName: headerParamName}]
  putCookieRule: (token, cookieName) ->
    if !(@transformerJson.cookieSubstitutions?)
      @transformerJson.cookieSubstitutions = []
    @transformerJson.cookieSubstitutions = [{token : token, paramName: cookieName}]
  getQueryParamRule: () ->
      if  @transformerJson.queryParamSubstitutions? && @transformerJson.queryParamSubstitutions.length == 1
          return @transformerJson.queryParamSubstitutions[0]
      else
          return {}
  getHeaderRule: () ->
    if  @transformerJson.headerSubstitutions? && @transformerJson.headerSubstitutions.length == 1
      return @transformerJson.headerSubstitutions[0]
    else
      return {}
  getCookieRule: () ->
    if  @transformerJson.cookieSubstitutions? && @transformerJson.cookieSubstitutions.length == 1
      return @transformerJson.cookieSubstitutions[0]
    else
      return {}
  getJsonData: ()->
    JSON.stringify(self.transformerJson)

class View
  self = null
  controller = null
  constructor: (@model) ->
    self = this
    $("#update").click((event)->controller.save())
    $(".close-button").click((event)-> $("#success").hide();$("#error").hide())
    $("#set").click((event)->controller.putQueryParamRule($("#token").val(), $("#queryParamName").val()))
    $("#setHeader").click((event)->controller.putHeaderParamRule($("#headerToken").val(), $("#headerParamName").val()))
    $("#setCookie").click((event)->controller.putCookieRule($("#cookieToken").val(), $("#cookieName").val()))
  setController: (newController) ->
    controller = newController
    self.update()
  saveError: () ->
    $("#error").show()
  saveSuccess: () ->
    $("#success").show()
  update: () ->
    rule = @model.getQueryParamRule()
    if(rule.token? && rule.paramName?)
        $("#rule").empty().text(rule.token.toString() + " -> " + rule.paramName.toString())
    else
        $("#rule").empty()
    headerRule = @model.getHeaderRule()
    if(headerRule.token? && headerRule.paramName?)
      $("#headerRule").empty().text(headerRule.token.toString() + " -> " + headerRule.paramName.toString())
    else
      $("#headerRule").empty()
    cookieRule = @model.getCookieRule()
    if(cookieRule.token? && cookieRule.paramName?)
      $("#cookieRule").empty().text(cookieRule.token.toString() + " -> " + cookieRule.paramName.toString())
    else
      $("#cookieRule").empty()


class Controller
  me = null
  constructor: (@model, @view) ->
    me = this
  putQueryParamRule: (token, queryParamName) ->
    if token? && queryParamName? && token.length > 1 && queryParamName.length > 1
        @model.putQueryParamRule(token, queryParamName)
    @view.update()
  putHeaderParamRule: (token, headerParamName) ->
    if token? && headerParamName? && token.length > 1 && headerParamName.length > 1
      @model.putHeaderParamRule(token, headerParamName)
    @view.update()
  putCookieRule: (token, cookieName) ->
    if token? && cookieName? && token.length > 1 && cookieName.length > 1
      @model.putCookieRule(token, cookieName)
    @view.update()
  save: ()->
    $.ajax({type:"PUT",contentType:"application/json", data:@model.getJsonData(),error:me.onError,success:me.onSuccess})
  onError: ()->
    me.view.saveError()
  onSuccess: ()->
    me.view.saveSuccess()



window.transformer = (transformerJson, responderName)->
    transformer = new Transformer transformerJson, responderName
    view = new View transformer
    controller = new Controller transformer, view
    view.setController(controller)