$version: "2"

namespace talks.api

use alloy#simpleRestJson

@simpleRestJson
service TalkService {
    operations: [ExtendPlaylist]
}

@http(method: "POST", uri: "/api/playlist/extend")
operation ExtendPlaylist {
    input := {
        @required
        playlist: TalkIds

        @required
        @range(min: 1)
        count: Integer
    }

    output := {
        @required
        talks: Talks
    }
}

string TalkId

list TalkIds {
    member: TalkId
}

structure Talk {
    @required
    id: TalkId

    @required
    conference: String

    @required
    title: String

    @required
    speakers: Strings

    @required
    url: String

    @required
    thumbnailUrl: String

    abstract: String

    @required
    tags: Strings
}

list Talks {
    member: Talk
}

list Strings {
    member: String
}
