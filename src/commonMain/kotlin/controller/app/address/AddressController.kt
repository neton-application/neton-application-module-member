package controller.app.address

import controller.app.address.dto.CreateAddressRequest
import controller.app.address.dto.UpdateAddressRequest
import model.Address
import neton.core.annotations.*
import neton.core.http.NotFoundException
import neton.core.interfaces.Identity
import neton.database.dsl.*
import table.AddressTable

@Controller("/member/address")
class AddressController {

    @Post("/create")
    suspend fun create(identity: Identity, @Body request: CreateAddressRequest): Long {
        val userId = identity.id.toLong()
        if (request.defaultStatus == 1) {
            clearDefaultAddress(userId)
        }
        val address = Address(
            userId = userId,
            name = request.name,
            mobile = request.mobile,
            areaCode = request.areaCode,
            detailAddress = request.detailAddress,
            defaultStatus = request.defaultStatus,
        )
        return AddressTable.insert(address).id
    }

    @Put("/update")
    suspend fun update(identity: Identity, @Body request: UpdateAddressRequest) {
        val userId = identity.id.toLong()
        val existing = AddressTable.get(request.id)
            ?.takeIf { it.userId == userId }
            ?: throw NotFoundException("Address not found: ${request.id}")
        if (request.defaultStatus == 1) {
            clearDefaultAddress(userId)
        }
        AddressTable.update(
            existing.copy(
                name = request.name,
                mobile = request.mobile,
                areaCode = request.areaCode,
                detailAddress = request.detailAddress,
                defaultStatus = request.defaultStatus,
            )
        )
    }

    @Delete("/delete/{id}")
    suspend fun delete(identity: Identity, @PathVariable id: Long) {
        val userId = identity.id.toLong()
        val existing = AddressTable.get(id)
            ?.takeIf { it.userId == userId }
            ?: throw NotFoundException("Address not found: $id")
        AddressTable.destroy(existing.id)
    }

    @Get("/get/{id}")
    suspend fun get(identity: Identity, @PathVariable id: Long): Address? {
        val userId = identity.id.toLong()
        return AddressTable.get(id)?.takeIf { it.userId == userId }
    }

    @Get("/list")
    suspend fun list(identity: Identity): List<Address> {
        val userId = identity.id.toLong()
        return AddressTable.query {
            where {
                Address::userId eq userId
            }
            orderBy(Address::defaultStatus.desc(), Address::id.desc())
        }.list()
    }

    @Get("/get-default")
    suspend fun getDefault(identity: Identity): Address? {
        val userId = identity.id.toLong()
        return AddressTable.oneWhere {
            and(
                Address::userId eq userId,
                Address::defaultStatus eq 1
            )
        }
    }

    private suspend fun clearDefaultAddress(userId: Long) {
        val defaults = AddressTable.query {
            where {
                and(
                    Address::userId eq userId,
                    Address::defaultStatus eq 1
                )
            }
        }.list()
        for (addr in defaults) {
            AddressTable.update(addr.copy(defaultStatus = 0))
        }
    }
}
