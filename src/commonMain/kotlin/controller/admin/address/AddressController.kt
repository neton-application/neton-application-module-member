package controller.admin.address

import model.Address
import table.AddressTable
import neton.database.dsl.*
import neton.core.annotations.Controller
import neton.core.annotations.Get

@Controller("/member/address")
class AddressController {

    @Get("/list")
    suspend fun list(userId: Long): List<Address> {
        return AddressTable.query {
            where {
                Address::userId eq userId
            }
            orderBy(Address::defaultStatus.desc(), Address::id.desc())
        }.list()
    }
}
