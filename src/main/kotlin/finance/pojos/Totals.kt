package finance.pojos

import java.math.BigDecimal

class Totals {
    var totals: BigDecimal = BigDecimal(0.0)
    set(value) {
        field = value.setScale(2, BigDecimal.ROUND_HALF_UP)
    }
    var totalsCleared: BigDecimal = BigDecimal(0.0)
    set(value) {
        field = value.setScale(2, BigDecimal.ROUND_HALF_UP)
    }
}
