$version: "2.1"

namespace talks.api

use alloy#simpleRestJson

@simpleRestJson
service PreviewService {
    operations: [
        GetDefaultRules
        Preview
    ]
}

@readonly
@http(method: "GET", uri: "/api/preview/default-rules")
operation GetDefaultRules {
    input := {}

    output := {
        @required
        rules: [Rule]
    }
}

@http(method: "POST", uri: "/api/preview")
operation Preview {
    input := {
        @required
        rules: [Rule]

        @required
        seed: Long

        @required
        @range(min: 1)
        count: Integer = 5

        @required
        history: [TalkId] = []
    }

    output := {
        @required
        trace: [GenerationStep]
    }

    errors: [
        UnknownTalk
    ]
}

@error("client")
@httpError(400)
structure UnknownTalk {
    @required
    talkId: TalkId
}
