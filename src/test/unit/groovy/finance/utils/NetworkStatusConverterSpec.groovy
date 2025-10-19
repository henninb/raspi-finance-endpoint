package finance.utils

import finance.domain.NetworkStatus
import spock.lang.Specification

class NetworkStatusConverterSpec extends Specification {
    def converter = new NetworkStatusConverter()

    def "convertToEntityAttribute maps known values case-insensitively and trims"() {
        expect:
        converter.convertToEntityAttribute(input) == expected

        where:
        input             || expected
        'in_network'      || NetworkStatus.InNetwork
        ' In_Network '    || NetworkStatus.InNetwork
        'out_of_network'  || NetworkStatus.OutOfNetwork
        ' UNKNOWN '       || NetworkStatus.Unknown
    }

    def "convertToDatabaseColumn returns enum label"() {
        expect:
        converter.convertToDatabaseColumn(ns) == ns.getLabel()

        where:
        ns << [NetworkStatus.InNetwork, NetworkStatus.OutOfNetwork, NetworkStatus.Unknown]
    }

    def "invalid input throws RuntimeException"() {
        when:
            converter.convertToEntityAttribute('not-a-status')

        then:
            def ex = thrown(RuntimeException)
            ex.message == 'Unknown network status attribute: not-a-status'
    }

    def "round trip conversion works for all NetworkStatus values"() {
        expect:
        [NetworkStatus.InNetwork, NetworkStatus.OutOfNetwork, NetworkStatus.Unknown].every { original ->
            String db = converter.convertToDatabaseColumn(original)
            converter.convertToEntityAttribute(db) == original
        }
    }
}
