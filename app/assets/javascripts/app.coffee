headerButtonToFormInput = {}
headerNames = []
count = 0

handleDelete = (count,name) ->
  () ->
    $("#headers_"+count+"__name").remove()
    $("#headers_"+count+"__value").remove()
    $("#header_"+count).remove()
    index = headerNames.indexOf(name)
    headerNames.splice(index, 1)

window.addHeader = (name, value) ->
  headerNames.push(name)
  $("#createResponderForm").append("<input style=\"display:none\" type=\"text\" id=\"headers_" + count + "__name\" name=\"headers[" + count + "].name\" value=\"" + name + "\"><input style=\"display:none\" type=\"text\" id=\"headers_" + count + "__value\" name=\"headers[" + count + "].value\" value=\"" + value + "\">")
  $("#headers").append("<li id=\"header_"+count+"\">"+name+"->"+value+" <a id=\"delete_"+count+"\" class=\"btn btn-mini\">delete</a></li>")
  $("#delete_"+count.toString()).click(handleDelete(count,name))
  count++

handleAdd = ->
  toCreate =
    name: $("#header_name").val()
    value: $("#header_value").val()
  if toCreate.name? && toCreate.value?  && headerNames.indexOf(toCreate.name) == -1
    addHeader(toCreate.name,toCreate.value)

$ ->
  $("#addHeader").click(handleAdd)
  $("#modal").hide()