package table

import model.MemberInviteRecord
import model.MemberInviteRecordTableImpl
import neton.database.api.Table

object MemberInviteRecordTable : Table<MemberInviteRecord, Long> by MemberInviteRecordTableImpl
