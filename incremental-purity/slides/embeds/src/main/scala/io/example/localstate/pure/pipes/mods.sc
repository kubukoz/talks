val allFlights = findFlights(date)

val mods = NonEmptyList.of[Pipe[Flight]](
  expensify,
  skipCheapest(allFlights.size)
)

<span class="fragment">mods.reduceK //Pipe[Flight]</span>
<span class="fragment">  .apply(allFlights) //List[Flight]</span>
