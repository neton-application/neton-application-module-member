package table

import model.MemberInviteCode
import model.MemberInviteCodeTableImpl
import neton.database.api.Table

object MemberInviteCodeTable : Table<MemberInviteCode, Long> by MemberInviteCodeTableImpl
