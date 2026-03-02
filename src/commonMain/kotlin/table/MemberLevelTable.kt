package table

import model.MemberLevel
import model.MemberLevelTableImpl
import neton.database.api.Table

object MemberLevelTable : Table<MemberLevel, Long> by MemberLevelTableImpl
