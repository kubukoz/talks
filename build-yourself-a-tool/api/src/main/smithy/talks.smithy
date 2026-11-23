$version: "2.1"

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
        talks: [Talk]
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
    speakers: [Speaker]

    @required
    url: String

    @required
    durationSeconds: Integer

    @required
    thumbnailUrl: String

    abstract: String

    @required
    tags: [String]
}

structure Speaker {
    @required
    name: String

    country: CountryCode
}

@pattern("^[A-Z]{2}$")
string CountryCode
