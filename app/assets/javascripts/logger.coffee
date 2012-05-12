modalOn = (headers) ->
  () ->
    $("#modalBody").empty()
    $("#modalBody").append(prettyPrint(headers))
    $("#modal").modal()

window.message = (msg) ->
  window.latest = msg
  messages = $('#messages')
  p = $(document.createElement("p"))
  p.text(msg.message + " ")
  a = $(document.createElement("a"))
  a.text("Headers")
  a.click(modalOn(msg))
  p.append(a)
  messages.prepend(p)