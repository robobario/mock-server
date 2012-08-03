class Header
    constructor: (@name,@value) ->

class Headers
    count = 0
    headers = {}
    self = null

    constructor: () ->
        self = this

    pushHeader : (header,index)->
        headers[header.name]=index

    remove: (header) ->
        index = headers[header.name]
        if index?
            $("input[name='headers["+index+"].name']").remove()
            $("input[name='headers["+index+"].value']").remove()
            $("#header_"+index).remove()
            headers[header] = null

    add: (header) ->
        self.remove(header)
        self.pushHeader(header,count)
        $(".responder-form").append("<input style=\"display:none\" type=\"text\" name=\"headers[" + count + "].name\" value=\"" + header.name + "\"><input style=\"display:none\" type=\"text\" name=\"headers[" + count + "].value\" value=\"" + header.value + "\">")
        $("#headers").append("<li id=\"header_"+count.toString()+"\">"+header.name+"->"+header.value+" <a id=\"delete_"+count+"\" class=\"btn btn-mini\">delete</a></li>")
        $("#delete_"+count.toString()).click(() -> self.remove(header))
        count++

headers = new Headers
window.addHeader = (headerName,headerVal) ->
  headers.add(new Header headerName, headerVal)

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
     headers.add(new Header "Content-Type",event.target.name)
  )

