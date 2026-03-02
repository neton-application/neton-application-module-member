package table

import model.MemberGroup
import model.MemberGroupTableImpl
import neton.database.api.Table

object MemberGroupTable : Table<MemberGroup, Long> by MemberGroupTableImpl
