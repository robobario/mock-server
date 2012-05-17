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

class View
    self = null
    controller = null
    constructor: (@model) ->
        self = this
        $("a").click((event)->controller.putQueryParamRule($("#token").val(), $("#queryParamName").val()))
    setController: (newController) ->
        controller = newController
        self.update()
    update: () ->
        rule = @model.getRule()
        if(rule.token? && rule.queryParamName?)
            $("#rule").empty().text(rule.token.toString() + rule.queryParamName.toString())
        else
            $("#rule").empty()


class Controller
    self = null
    constructor: (@model, @view) ->
        self = this

    putQueryParamRule: (token, queryParamName) ->
        if token? && queryParamName? && token.length > 1 && queryParamName.length > 1
            @model.putQueryParamRule(token, queryParamName)
        @view.update()



window.transformer = (transformerJson, responderName)->
    transformer = new Transformer transformerJson, responderName
    view = new View transformer
    controller = new Controller transformer, view
    view.setController(controller)