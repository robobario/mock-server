class Transformer
  self = null
  constructor: (@transformerJson, @responderName) ->
      self = this
  putQueryParamRule: (token, queryParamName) ->
      if !(@transformerJson.queryParamSubstitutions?)
          @transformerJson.queryParamSubstitutions = []
      @transformerJson.queryParamSubstitutions = [{token : token, queryParamName: queryParamName}]
  getRule: () ->
      if  @transformerJson.queryParamSubstitutions? && @transformerJson.queryParamSubstitutions.length == 1
          return @transformerJson.queryParamSubstitutions[0]
      else
          return {}
  getJsonData: ()->
    JSON.stringify(@transformerJson)

class View
  self = null
  controller = null
  constructor: (@model) ->
    self = this
    $("#set").click((event)->controller.putQueryParamRule($("#token").val(), $("#queryParamName").val()))
    $("#update").click((event)->controller.save())
    $(".close-button").click((event)-> $("#success").hide();$("#error").hide())
  setController: (newController) ->
    controller = newController
    self.update()
  saveError: () ->
    $("#error").show()
  saveSuccess: () ->
    $("#success").show()
  update: () ->
    rule = @model.getRule()
    if(rule.token? && rule.queryParamName?)
        $("#rule").empty().text(rule.token.toString() + " -> " + rule.queryParamName.toString())
    else
        $("#rule").empty()


class Controller
  me = null
  constructor: (@model, @view) ->
    me = this
  putQueryParamRule: (token, queryParamName) ->
    if token? && queryParamName? && token.length > 1 && queryParamName.length > 1
        @model.putQueryParamRule(token, queryParamName)
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