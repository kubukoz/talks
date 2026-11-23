$version: "2"

namespace hello

use alloy#simpleRestJson

@simpleRestJson
service HelloService {
    operations: [
        Hello
    ]
}

@readonly
@http(method: "GET", uri: "/api/hello/{name}")
operation Hello {
    input := {
        @required
        @httpLabel
        name: String
    }

    output := {
        @required
        greeting: String
    }
}
