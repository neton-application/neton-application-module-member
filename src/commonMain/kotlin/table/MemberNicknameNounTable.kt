package table

import model.MemberNicknameNoun
import model.MemberNicknameNounTableImpl
import neton.database.api.Table

object MemberNicknameNounTable : Table<MemberNicknameNoun, Long> by MemberNicknameNounTableImpl
