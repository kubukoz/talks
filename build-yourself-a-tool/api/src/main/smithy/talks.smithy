$version: "2"

namespace talks.api

use alloy#simpleRestJson
use alloy#uuidFormat

@simpleRestJson
service TalkService {
    operations: [
        CreatePlaylist
        ExtendPlaylist
    ]
}

@http(method: "POST", uri: "/api/playlists")
operation CreatePlaylist {
    input := {}

    output := {
        @required
        playlistId: PlaylistId

        @required
        talks: Talks
    }
}

@http(method: "POST", uri: "/api/playlists/{playlistId}/extend")
operation ExtendPlaylist {
    input := {
        @required
        @httpLabel
        playlistId: PlaylistId
    }

    output := {
        talk: Talk
    }

    errors: [
        PlaylistNotFound
    ]
}

@error("client")
@httpError(404)
structure PlaylistNotFound {
    @required
    playlistId: PlaylistId
}

@uuidFormat
string PlaylistId

string TalkId

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

union Rule {
    noRepeats: NoRepeats
}

structure NoRepeats {}
