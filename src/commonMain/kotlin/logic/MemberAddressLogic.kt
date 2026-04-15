package logic

import controller.app.address.dto.CreateAddressRequest
import controller.app.address.dto.UpdateAddressRequest
import model.Address
import table.AddressTable
import neton.core.http.NotFoundException
import neton.database.dsl.*

import neton.logging.Logger

class MemberAddressLogic(
    private val log: Logger
) {

    suspend fun create(userId: Long, request: CreateAddressRequest): Long {
        if (request.defaultStatus == 1) {
            clearDefaultAddress(userId)
        }
        return AddressTable.insert(Address(
            userId = userId,
            name = request.name,
            mobile = request.mobile,
            areaCode = request.areaCode,
            detailAddress = request.detailAddress,
            defaultStatus = request.defaultStatus,
        )).id
    }

    suspend fun update(userId: Long, request: UpdateAddressRequest) {
        val existing = AddressTable.get(request.id)
            ?.takeIf { it.userId == userId }
            ?: throw NotFoundException("Address not found: ${request.id}")

        if (request.defaultStatus == 1) {
            clearDefaultAddress(userId)
        }
        AddressTable.update(existing.copy(
            name = request.name,
            mobile = request.mobile,
            areaCode = request.areaCode,
            detailAddress = request.detailAddress,
            defaultStatus = request.defaultStatus
        ))
    }

    suspend fun delete(userId: Long, id: Long) {
        val existing = AddressTable.get(id)
            ?.takeIf { it.userId == userId }
            ?: throw NotFoundException("Address not found: $id")
        AddressTable.destroy(existing.id)
    }

    suspend fun getById(userId: Long, id: Long): Address? {
        return AddressTable.get(id)?.takeIf { it.userId == userId }
    }

    suspend fun listByUser(userId: Long): List<Address> {
        return AddressTable.query {
            where { Address::userId eq userId }
            orderBy(Address::defaultStatus.desc(), Address::id.desc())
        }.list()
    }

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
