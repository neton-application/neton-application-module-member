package controller.app.address

import model.Address
import table.AddressTable
import neton.database.dsl.*

import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete

@Controller("/member/address")
class AddressController {

    @Post("/create")
    suspend fun create(address: Address): Long {
        // If setting as default, clear other defaults first
        if (address.defaultStatus == 1) {
            clearDefaultAddress(address.userId)
        }
        return AddressTable.insert(address).id
    }

    @Put("/update")
    suspend fun update(address: Address) {
        // If setting as default, clear other defaults first
        if (address.defaultStatus == 1) {
            clearDefaultAddress(address.userId)
        }
        AddressTable.update(address)
    }

    @Delete("/delete")
    suspend fun delete(id: Long) {
        AddressTable.destroy(id)
    }

    @Get("/get")
    suspend fun get(id: Long): Address? {
        return AddressTable.get(id)
    }

    @Get("/list")
    suspend fun list(userId: Long): List<Address> {
        return AddressTable.query {
            where {
                Address::userId eq userId
            }
            orderBy(Address::defaultStatus.desc(), Address::id.desc())
        }.list()
    }

    @Get("/get-default")
    suspend fun getDefault(userId: Long): Address? {
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
