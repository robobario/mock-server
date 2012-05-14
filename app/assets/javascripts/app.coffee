class Header
    constructor: (@name,@value) ->

class Headers
    count = 0
    headers = {}

    constructor: () ->

    pushHeader : (header,index)->
        headers[header]=index

    remove: (header) ->
        index = headers[header]
        if index?
            $(".headers_"+index+"__name").remove()
            $(".headers_"+index+"__value").remove()
            $(".header_"+index).remove()
            delete headerNames[header]
    removeFunc: (header) ->
        remove(header)

    add: (header) ->
        this.remove(header)
        this.pushHeader(header,count)
        $(".responder-form").append("<input style=\"display:none\" type=\"text\" name=\"headers[" + count + "].name\" value=\"" + header.name + "\"><input style=\"display:none\" type=\"text\" name=\"headers[" + count + "].value\" value=\"" + header.value + "\">")
        $("#headers").append("<li id=\"header_"+count+"\">"+header.name+"->"+header.value+" <a id=\"delete_"+count+"\" class=\"btn btn-mini\">delete</a></li>")
        $("#delete_"+count.toString()).click(this.removeFunc)
        count++

headers = new Headers

handleAdd = ->
  toCreate = new Header $("#header_name").val(), $("#header_value").val()
  headers.add(toCreate)

$ ->
  $("#addHeader").click(handleAdd)
  $("#modal").hide()
  $('.button-radio').button()
  $('#textResponse').button()
  $('#textResponse').click((event)->
    $("#fileForm").hide()
    $('#bodyForm').show()
  )
  $('#fileResponse').click((event)->
    $('#fileForm').show()
    $('#bodyForm').hide()
  )
  $('.dropdown-toggle').dropdown()
  $('.content-type-dropdown').click((event)->
     window.addHeader(Header "Content-Type",event.target.name)
  )

