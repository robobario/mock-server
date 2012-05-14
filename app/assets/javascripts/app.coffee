headerButtonToFormInput = {}
headerNames = {}

handleDelete = (name) ->
  ()->
    index = headerNames[name]
    if index?
      $("#headers_"+index+"__name").remove()
      $("#headers_"+index+"__value").remove()
      $("#header_"+index).remove()
      delete headerNames[name]

pushHeader = (name, index) ->
  headerNames[name]=index

window.addHeader = (name, value) ->
  count = 0
  handleDelete(name)()
  pushHeader(name,count)
  $("form").append("<input style=\"display:none\" type=\"text\" id=\"headers_" + count + "__name\" name=\"headers[" + count + "].name\" value=\"" + name + "\"><input style=\"display:none\" type=\"text\" id=\"headers_" + count + "__value\" name=\"headers[" + count + "].value\" value=\"" + value + "\">")
  $("#headers").append("<li id=\"header_"+count+"\">"+name+"->"+value+" <a id=\"delete_"+count+"\" class=\"btn btn-mini\">delete</a></li>")
  $("#delete_"+count.toString()).click(handleDelete(name))
  count++

handleAdd = ->
  toCreate =
    name: $("#header_name").val()
    value: $("#header_value").val()
  addHeader(toCreate.name,toCreate.value)

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
     window.addHeader("Content-Type",event.target.name)
  )