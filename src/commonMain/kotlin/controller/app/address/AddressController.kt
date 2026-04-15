package controller.app.address

import controller.app.address.dto.CreateAddressRequest
import controller.app.address.dto.UpdateAddressRequest
import logic.MemberAddressLogic
import neton.core.annotations.*
import neton.core.interfaces.Identity

@Controller("/app/member/address")
class AddressController(
    private val addressLogic: MemberAddressLogic
) {

    @Post("/create")
    suspend fun create(identity: Identity, @Body request: CreateAddressRequest): Long =
        addressLogic.create(identity.id.toLong(), request)

    @Put("/update")
    suspend fun update(identity: Identity, @Body request: UpdateAddressRequest) =
        addressLogic.update(identity.id.toLong(), request)

    @Delete("/delete/{id}")
    suspend fun delete(identity: Identity, @PathVariable id: Long) =
        addressLogic.delete(identity.id.toLong(), id)

    @Get("/get/{id}")
    suspend fun get(identity: Identity, @PathVariable id: Long) =
        addressLogic.getById(identity.id.toLong(), id)

    @Get("/list")
    suspend fun list(identity: Identity) =
        addressLogic.listByUser(identity.id.toLong())

    @Get("/get-default")
    suspend fun getDefault(identity: Identity) =
        addressLogic.getDefault(identity.id.toLong())
}
