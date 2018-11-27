val expensify: Pipe[Flight] =
  if (isDateSoon(date))
    _.map(Flight.price.modify(_ * 6))
      .map(Flight.iconColor.set(Color.Red))
  else identity
