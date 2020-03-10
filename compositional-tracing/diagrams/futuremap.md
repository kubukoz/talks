```plantuml
@startuml
'!include stylesheet.iuml

start
:42;
partition "on ec1" {
  :val x = f(42);
}
partition "on ec2" {
  :val y = g(x);
}
stop
@enduml
```
