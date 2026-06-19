package table

import model.MemberNicknameAdjective
import model.MemberNicknameAdjectiveTableImpl
import neton.database.api.Table

object MemberNicknameAdjectiveTable : Table<MemberNicknameAdjective, Long> by MemberNicknameAdjectiveTableImpl
