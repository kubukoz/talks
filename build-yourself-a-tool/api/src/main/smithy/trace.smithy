$version: "2.1"

namespace talks.api

structure GenerationStep {
    @required
    position: Integer

    talk: Talk

    @required
    evaluation: Evaluation

    @required
    durationNanos: Long
}

structure Evaluation {
    @required
    matching: [Talk]

    @required
    rules: [RuleEvaluation]
}

structure RuleEvaluation {
    @required
    rule: Rule

    @required
    outcome: RuleOutcome

    @required
    matchedBefore: Integer

    @required
    matchedAfter: Integer

    @required
    rejected: [Talk]

    @required
    durationNanos: Long
}

enum RuleOutcome {
    APPLIED
    SKIPPED
    INACTIVE
}
