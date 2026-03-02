package controller.admin.member

import model.MemberConfig
import table.MemberConfigTable
import neton.database.dsl.*
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Put

@Controller("/member/config")
class MemberConfigController {

    @Put("/save")
    suspend fun save(config: MemberConfig) {
        val existing = MemberConfigTable.oneWhere {
            MemberConfig::configKey eq config.configKey
        }

        if (existing != null) {
            MemberConfigTable.update(existing.copy(value = config.value))
        } else {
            MemberConfigTable.insert(config)
        }
    }

    @Get("/get")
    suspend fun get(configKey: String): MemberConfig? {
        return MemberConfigTable.oneWhere {
            MemberConfig::configKey eq configKey
        }
    }
}
