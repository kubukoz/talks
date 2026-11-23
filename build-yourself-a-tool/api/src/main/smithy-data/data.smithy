$version: "2"

namespace talks.data

use talks.api#Strings
use talks.api#TalkId

structure TalkRecord {
    @required
    id: TalkId

    @required
    title: String

    @required
    speakers: Strings

    @required
    url: String

    @required
    thumbnail: String

    abstract: String

    @required
    tags: Strings
}

list TalkRecords {
    member: TalkRecord
}
