mock-server
===========
Mock server is intended to make it easy to create http resources that will respond with a given body, code and headers. It can also replace tokens in the body with variables from the request. The purpose is to assist with manual testing.

To run it in development mode : ./sbt.sh play run
To run in production mode : ./sbt.sh play start

By default it will write out responders into /tmp/mockserve to configure this directory add a line to ./conf/application.conf like :

document.base=/opt/junkz
