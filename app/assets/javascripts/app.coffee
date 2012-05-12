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


handleAdd = ->
  toCreate =
    name: $("#header_name").val()
    value: $("#header_value").val()
  if toCreate.name? && toCreate.value?  && headerNames.indexOf(toCreate.name) == -1
    headerNames.push(toCreate.name)
    $("#createResponderForm").append("<input style=\"display:none\" type=\"text\" id=\"headers_" + count + "__name\" name=\"headers[" + count + "].name\" value=\"" + toCreate.name + "\"><input style=\"display:none\" type=\"text\" id=\"headers_" + count + "__value\" name=\"headers[" + count + "].value\" value=\"" + toCreate.value + "\">")
    $("#headers").append("<li id=\"header_"+count+"\">"+toCreate.name+"->"+toCreate.value+" <a id=\"delete_"+count+"\" class=\"btn btn-mini\">delete</a></li>")
    $("#delete_"+count.toString()).click(handleDelete(count,toCreate.name))
    count++

$ ->
  $("#addHeader").click(handleAdd)
  $("#modal").hide()