package table

import model.MemberSignInConfig
import model.MemberSignInConfigTableImpl
import neton.database.api.Table

object MemberSignInConfigTable : Table<MemberSignInConfig, Long> by MemberSignInConfigTableImpl
