package table

import model.MemberLevelRecord
import model.MemberLevelRecordTableImpl
import neton.database.api.Table

object MemberLevelRecordTable : Table<MemberLevelRecord, Long> by MemberLevelRecordTableImpl
