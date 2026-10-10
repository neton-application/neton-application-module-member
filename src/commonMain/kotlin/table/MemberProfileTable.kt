package table

import model.MemberProfile
import model.MemberProfileTableImpl
import neton.database.api.Table

object MemberProfileTable : Table<MemberProfile, Long> by MemberProfileTableImpl
