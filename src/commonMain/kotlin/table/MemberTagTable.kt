package table

import model.MemberTag
import model.MemberTagTableImpl
import neton.database.api.Table

object MemberTagTable : Table<MemberTag, Long> by MemberTagTableImpl
