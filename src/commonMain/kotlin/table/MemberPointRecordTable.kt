package table

import model.MemberPointRecord
import model.MemberPointRecordTableImpl
import neton.database.api.Table

object MemberPointRecordTable : Table<MemberPointRecord, Long> by MemberPointRecordTableImpl
