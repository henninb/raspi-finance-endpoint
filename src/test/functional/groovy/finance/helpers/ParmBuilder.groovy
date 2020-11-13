package finance.helpers

import finance.domain.Parm

class ParmBuilder {

    String parmName = 'foo'
    String parmValue = 'bar'

    static ParmBuilder builder() {
        return new ParmBuilder()
    }

    Parm build() {
        Parm parm = new Parm()
        parm.parmName = parmName
        parm.parmValue = parmValue
        return parm
    }

    ParmBuilder parmName(parmName) {
        this.parmName = parmName
        return this
    }

    ParmBuilder parmValue(parmValue) {
        this.parmValue = parmValue
        return this
    }
}
