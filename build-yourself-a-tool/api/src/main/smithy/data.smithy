$version: "2.1"

namespace talks.data

use talks.api#Speaker
use talks.api#TalkId

structure TalkRecord {
    @required
    id: TalkId

    @required
    title: String

    @required
    speakers: [Speaker]

    @required
    url: String

    @required
    durationSeconds: Integer

    @required
    thumbnail: String

    abstract: String

    @required
    tags: [String]
}

list TalkRecords {
    member: TalkRecord
}
