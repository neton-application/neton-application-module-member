package table

import model.Report
import model.ReportTableImpl
import neton.database.api.Table

object ReportTable : Table<Report, Long> by ReportTableImpl
