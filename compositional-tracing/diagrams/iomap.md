```plantuml
@startuml
'!include stylesheet.iuml

'
'runWithContext (IO)
'

start
partition "context: root" {
  :val rootContext = getContext();
  :setContext(someContext);
  partition "context: someContext" {
    :actualProcessing;
    :setContext(rootContext);
  }
}
stop
@enduml
```
---

```plantuml
@startuml
'!include stylesheet.iuml

'
'Instrumented EC
'


group mdcAware(ec1).execute(() => f(42)):
"ec-0" -> "ec-0": val rootContext = getContext()
"ec-0" -> "ec-1": schedule runnable
group runWithContext(rootContext)
"ec-1" -> "ec-1": val storedContext = getContext()
"ec-1" -> "ec-1": setContext(rootContext)
"ec-1" -> "ec-1": val x = f(42)
"ec-1" -> "ec-1": setContext(storedContext)
end
end

@enduml
```

---

```scala
withContext(newContext)(launchMissiles)
```

Assume starting with root context

```plantuml
@startuml launchmissilies-noshift
'!include stylesheet.iuml
group withContext
"thread-1" -> "thread-1": val root = getContext()
"thread-1" -> "thread-1": setContext(newContext)
group fa
"thread-1" -> "thread-1": launch missiles
end
"thread-1" -> "thread-1": setContext(root)
end
@enduml
```

---

```scala
withContext(newContext)(IO.shift(mdcAware(ec)) *> launchMissiles)
```

```plantuml
@startuml launchmissiles-shift
'!include stylesheet.iuml

participant "thread-1"
participant "thread-2"

group withContext
"thread-1" -> "thread-1": val root = getContext()
"thread-1" -> "thread-1": setContext(newContext)
group fa
"thread-1" -> "thread-2": shift
end
"thread-1" -> "thread-1": setContext(root)
"thread-2" -> "thread-2": val root = getContext()
"thread-2" -> "thread-2": setContext(newContext)
group fa
"thread-2" -> "thread-2": launch missiles
end
"thread-2" -> "thread-2": setContext(root)
end
@enduml
```
---

```scala
withContext(newContext)(launchMissiles) *> remainder
```

```plantuml
@startuml launchmissiles-noshift-async
'!include stylesheet.iuml

group withContext
group runWithContext(newContext)
"thread-1" -> "thread-1": val root = getContext()
"thread-1" -> "thread-1": setContext(newContext)
group fa
"thread-1" -> "thread-1": launch missiles
end
group runWithContext(root)
"thread-1" -> "thread-1": val root2 = getContext()
"thread-1" -> "thread-1": setContext(root)
group remainder
"thread-1" -> "thread-1": remainder
end
"thread-1" -> "thread-1": setContext(root)
end
"thread-1" -> "thread-1": setContext(root)
end
end
@enduml
```
---

```scala
withContext(newContext)(IO.shift *> launchMissiles) *> remainder
```

```plantuml
@startuml launchmissiles-shift-async
'!include stylesheet.iuml

group withContext
group runWithContext(newContext)
"thread-1" -> "thread-1": val root = getContext()
"thread-1" -> "thread-1": setContext(newContext)
group fa
"thread-1" -> "thread-2": shift
end
"thread-1" -> "thread-1": setContext(root)
end
group runWithContext(newContext)
"thread-2" -> "thread-2": val root2 = getContext()
"thread-2" -> "thread-2": setContext(newContext)
group fa
"thread-2" -> "thread-2": launchMissiles
end
"thread-2" -> "thread-2": setContext(root2)
end
group runWithContext(root)
"thread-2" -> "thread-2": val root2 = getContext()
"thread-2" -> "thread-2": setContext(root)
group remainder
"thread-2" -> "thread-2": remainder
end
"thread-2" -> "thread-2": setContext(root2)
end
end
@enduml
```
