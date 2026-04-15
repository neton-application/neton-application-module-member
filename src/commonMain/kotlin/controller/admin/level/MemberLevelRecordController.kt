package controller.admin.level

import logic.MemberLevelLogic
import model.MemberLevelRecord
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.PathVariable
import neton.core.annotations.Permission

@Controller("/member/level-record")
class MemberLevelRecordController(
    private val memberLevelLogic: MemberLevelLogic
) {

    @Get("/page")
    @Permission("member:level:page")
    suspend fun page(
        pageNo: Int = 1,
        pageSize: Int = 10,
        userId: Long? = null,
        levelId: Long? = null
    ) = memberLevelLogic.pageLevelRecords(pageNo, pageSize, userId, levelId)

    @Get("/get/{id}")
    @Permission("member:level:query")
    suspend fun get(@PathVariable id: Long): MemberLevelRecord? {
        return memberLevelLogic.getLevelRecord(id)
    }
}
