$version: "2.1"

namespace talks.api

use smithy4s.meta#adt

@adt
union Rule {
    noRepeats: NoRepeats
    require: Requirement
}

@mixin
structure Skippable {
    @default(false)
    skippable: Boolean
}

structure NoRepeats with [Skippable] {}

structure Requirement with [Skippable] {
    @required
    at: Positions

    @required
    talk: TalkPredicate
}

union Positions {
    every: Every
    all: Unit
}

structure Every {
    @required
    @range(min: 1)
    n: Integer
}

union TalkPredicate {
    speakerCountry: CountryCode
    hasTag: String
    conference: String
    maxDurationSeconds: Integer
}
