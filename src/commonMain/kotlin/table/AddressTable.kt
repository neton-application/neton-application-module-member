package table

import model.Address
import model.AddressTableImpl
import neton.database.api.Table

object AddressTable : Table<Address, Long> by AddressTableImpl
