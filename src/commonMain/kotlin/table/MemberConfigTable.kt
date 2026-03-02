package table

import model.MemberConfig
import model.MemberConfigTableImpl
import neton.database.api.Table

object MemberConfigTable : Table<MemberConfig, Long> by MemberConfigTableImpl
