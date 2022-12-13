package finance.domain

enum class ExcelFileColumn(val column: Int) {
    Guid(1),

    TransactionDate(2),

    Description(3),

    Category(4),

    Amount(5),

    TransactionState(6),

    Notes(7);

    companion object {
        fun fromInt(value: Int) = values().first { it.column == value }
    }
}