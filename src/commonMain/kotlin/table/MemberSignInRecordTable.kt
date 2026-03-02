package table

import model.MemberSignInRecord
import model.MemberSignInRecordTableImpl
import neton.database.api.Table

object MemberSignInRecordTable : Table<MemberSignInRecord, Long> by MemberSignInRecordTableImpl
