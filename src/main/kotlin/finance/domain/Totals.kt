package finance.domain

import java.math.BigDecimal
import java.math.RoundingMode

class Totals {
    var totals: BigDecimal = BigDecimal(0.0)
    set(value) {
        field = value.setScale(2, RoundingMode.FLOOR)
    }
    var totalsCleared: BigDecimal = BigDecimal(0.0)
    set(value) {
        field = value.setScale(2, RoundingMode.FLOOR)
    }
}
