$version: "2.1"

namespace talks.api

use smithy4s.meta#adt

@adt
union Rule {
    noRepeats: NoRepeats
    require: Requirement
    spacing: Spacing
}

@mixin
structure Skippable {
    @required
    skippable: Boolean = false
}

structure NoRepeats with [Skippable] {}

structure Requirement with [Skippable] {
    @required
    at: Positions

    @required
    talk: TalkPredicate
}

structure Spacing with [Skippable] {
    @required
    @range(min: 1)
    n: Integer

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
