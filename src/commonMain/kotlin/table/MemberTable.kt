package table

import model.Member
import model.MemberTableImpl
import neton.database.api.Table

object MemberTable : Table<Member, Long> by MemberTableImpl
