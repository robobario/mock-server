class Transformer
  self = {}
  constructor: (@transformer,@responderName) ->
    self = this

  addQueryParamRule: (token, queryParamName) ->
    if !transformer.queryParamSubstitutions?
      transformer.queryParamSubstitutions = []
    transformer.queryParamSubstitutions.push({"token":token,"queryParamName":queryParamName})

  removeQueryParamRule: (token, queryParamName) ->
    if transformer.queryParamSubstitutions?
      transformer.queryParamSubstitutions = transformer.queryParamSubstitutions.filter((item)->item.queryParamName isnt queryParamName && item.token isnt token)

class View
  self = null
  controller = null
  constructor: (@transformer) ->
    self = this
  setController: (newController) ->
    controller = newController
    self.update()
  update: () ->



class Controller
  self = null
  constructor: (@transformer,@view) ->
    self = this

  addQueryParamRule: (token, queryParamName) ->
    if token? && queryParamName? && token is String && queryParamName is String
      transformer.addQueryParamRule(token,queryParamName)

  removeQueryParamRule: (token, queryParamName) ->
    if token? && queryParamName? && token is String && queryParamName is String
      transformer.addQueryParamRule(token,queryParamName)


window.transformers = {
  transformerPage : (transformerJson,responderName) ->
    transformer = new Transformer transformerJson, responderName
    view = new View transformer
    controller = new Controller transformer, view
    view.setController(controller)
}