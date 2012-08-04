findToken = (ruleArray, search) ->
    toDelete = (i for rule,i in ruleArray when rule.token is search)
    if toDelete.length >= 1
        return toDelete[0]
    else
        return -1

deleteFromArray = (array, index) ->
    return (item for item,i in array when i isnt index)

class Transformer
  self = null
  constructor: (@transformerJson, @responderName) ->
      self = this
  addQueryParamRule: (token, queryParamName) ->
      if !(@transformerJson.queryParamSubstitutions?)
          @transformerJson.queryParamSubstitutions = []
      if findToken(@transformerJson.queryParamSubstitutions, token) is -1
        @transformerJson.queryParamSubstitutions.push({token : token, paramName: queryParamName})
  removeQueryParamRule: (token) ->
      if !(@transformerJson.queryParamSubstitutions?)
          @transformerJson.queryParamSubstitutions = []
      i = findToken(@transformerJson.queryParamSubstitutions,token)
      if i > -1
          @transformerJson.queryParamSubstitutions = deleteFromArray(@transformerJson.queryParamSubstitutions,i)
  getQueryParamRules: () ->
    if  @transformerJson.queryParamSubstitutions? && @transformerJson.queryParamSubstitutions.length >= 1
      return @transformerJson.queryParamSubstitutions
    else
      return []
  putHeaderParamRule: (token, headerParamName) ->
    if !(@transformerJson.headerSubstitutions?)
      @transformerJson.headerSubstitutions = []
    @transformerJson.headerSubstitutions = [{token : token, paramName: headerParamName}]
  getHeaderRule: () ->
    if  @transformerJson.headerSubstitutions? && @transformerJson.headerSubstitutions.length == 1
      return @transformerJson.headerSubstitutions[0]
    else
      return {}
  removeHeaderRule: (token) ->
    if !(@transformerJson.headerSubstitutions?)
      @transformerJson.headerSubstitutions = []
    i = findToken(@transformerJson.headerSubstitutions,token)
    if i > -1
      @transformerJson.headerSubstitutions = deleteFromArray(@transformerJson.headerSubstitutions,i)
  putCookieRule: (token, cookieName) ->
    if !(@transformerJson.cookieSubstitutions?)
      @transformerJson.cookieSubstitutions = []
    @transformerJson.cookieSubstitutions = [{token : token, paramName: cookieName}]
  getCookieRule: () ->
    if  @transformerJson.cookieSubstitutions? && @transformerJson.cookieSubstitutions.length == 1
      return @transformerJson.cookieSubstitutions[0]
    else
      return {}
  removeCookieRule: (token) ->
    if !(@transformerJson.cookieSubstitutions?)
      @transformerJson.cookieSubstitutions = []
    i = findToken(@transformerJson.cookieSubstitutions,token)
    if i > -1
      @transformerJson.cookieSubstitutions = deleteFromArray(@transformerJson.cookieSubstitutions,i)
  getJsonData: ()->
    JSON.stringify(self.transformerJson)

class View
  self = null
  controller = null
  constructor: (@model) ->
    self = this
    $("#update").click((event)->controller.save())
    $(".close-button").click((event)-> $("#success").hide();$("#error").hide())
    $("#set").click((event)->controller.addQueryParamRule($("#token").val(), $("#queryParamName").val()))
    $("#setHeader").click((event)->controller.putHeaderParamRule($("#headerToken").val(), $("#headerParamName").val()))
    $("#setCookie").click((event)->controller.putCookieRule($("#cookieToken").val(), $("#cookieName").val()))
  setController: (newController) ->
    controller = newController
    self.update()
  saveError: () ->
    $("#error").show()
  saveSuccess: () ->
    $("#success").show()
  addRule: (rule) ->
      if(rule.token? && rule.paramName?)
          $("#rule").append("<li name=\""+rule.token+"\">").children("li[name=\""+rule.token+"\"]").text(rule.token.toString() + " -> " + rule.paramName.toString()).append("<a href=\"#\" class=\"btn btn-mini btn-danger\">remove</a>").children("a").click(()->controller.removeQueryParamRule(rule.token))
  update: () ->
    rules = @model.getQueryParamRules()
    $("#rule").empty()
    self.addRule(rule) for rule in rules
    headerRule = @model.getHeaderRule()
    if(headerRule.token? && headerRule.paramName?)
      $("#headerRule").empty().append("<li>").children("li").text(headerRule.token.toString() + " -> " + headerRule.paramName.toString()).append("<a href=\"#\" class=\"btn btn-mini btn-danger\">remove</a>").children("a").click(()->controller.removeHeaderParamRule(headerRule.token))
    else
      $("#headerRule").empty()
    cookieRule = @model.getCookieRule()
    if(cookieRule.token? && cookieRule.paramName?)
      $("#cookieRule").empty().append("<li>").children("li").text(cookieRule.token.toString() + " -> " + cookieRule.paramName.toString()).append("<a href=\"#\" class=\"btn btn-mini btn-danger\">remove</a>").children("a").click(()->controller.removeCookieRule(cookieRule.token))
    else
      $("#cookieRule").empty()


class Controller
  me = null
  constructor: (@model, @view) ->
    me = this
  addQueryParamRule: (token, queryParamName) ->
    if token? && queryParamName? && token.length > 1 && queryParamName.length > 1
        @model.addQueryParamRule(token, queryParamName)
    @view.update()
  removeQueryParamRule: (token) ->
      if token?
          @model.removeQueryParamRule(token)
      @view.update()
  putHeaderParamRule: (token, headerParamName) ->
    if token? && headerParamName? && token.length > 1 && headerParamName.length > 1
      @model.putHeaderParamRule(token, headerParamName)
    @view.update()
  removeHeaderParamRule: (token) ->
    if token?
      @model.removeHeaderRule(token)
    @view.update()
  putCookieRule: (token, cookieName) ->
    if token? && cookieName? && token.length > 1 && cookieName.length > 1
      @model.putCookieRule(token, cookieName)
    @view.update()
  removeCookieRule: (token) ->
    if token?
      @model.removeCookieRule(token)
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