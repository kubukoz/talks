# Lightweight, functional microservices with http4s and doobie

It’s no secret that using full-blown frameworks that provide every functionality you could ask for
tends to be troublesome - some of them impose a certain structure on your applications,
hide implementation details of the parts of your program generated on the fly (making them
harder to debug), and require testing your programs in a specific way.
Thankfully, there are alternatives, such as not depending on frameworks to do
all the work - instead, you delegate parts of the functionality to libraries.
In this talk, I’m going to talk about two of them - http4s (a HTTP server/client library)
and doobie (a functional JDBC wrapper), how they interact together thanks to a common core
(cats-effect, fs2), and how they allow you to write resource-safe applications that you can fully control,
and whose parts you can easily test independently or as a whole.