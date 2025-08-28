package finance.domain

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import spock.lang.Shared
import spock.lang.Unroll

class NetworkStatusSpec extends BaseDomainSpec {

    @Shared
    protected String jsonPayloadInNetwork = '"in_network"'

    @Shared
    protected String jsonPayloadOutOfNetwork = '"out_of_network"'

    @Shared
    protected String jsonPayloadUnknown = '"unknown"'

    @Shared
    protected String jsonPayloadInvalid = '"invalid_status"'

    void 'test JSON deserialization to NetworkStatus'() {
        when:
        NetworkStatus status = mapper.readValue(jsonPayloadInNetwork, NetworkStatus)

        then:
        status == NetworkStatus.InNetwork
        status.label == "in_network"
        0 * _
    }

    @Unroll
    void 'test JSON serialization from NetworkStatus #networkStatus'() {
        when:
        String json = mapper.writeValueAsString(networkStatus)

        then:
        json == expectedJson
        0 * _

        where:
        networkStatus              | expectedJson
        NetworkStatus.InNetwork    | '"in_network"'
        NetworkStatus.OutOfNetwork | '"out_of_network"'
        NetworkStatus.Unknown      | '"unknown"'
    }

    @Unroll
    void 'test toString returns lowercase name for #networkStatus'() {
        when:
        String result = networkStatus.toString()

        then:
        result == expectedString
        0 * _

        where:
        networkStatus              | expectedString
        NetworkStatus.InNetwork    | 'innetwork'
        NetworkStatus.OutOfNetwork | 'outofnetwork'
        NetworkStatus.Unknown      | 'unknown'
    }

    @Unroll
    void 'test label property for #networkStatus'() {
        expect:
        networkStatus.label == expectedLabel

        where:
        networkStatus              | expectedLabel
        NetworkStatus.InNetwork    | 'in_network'
        NetworkStatus.OutOfNetwork | 'out_of_network'
        NetworkStatus.Unknown      | 'unknown'
    }

    @Unroll
    void 'test JSON deserialization for all valid values #jsonPayload'() {
        when:
        NetworkStatus status = mapper.readValue(jsonPayload, NetworkStatus)

        then:
        status == expectedStatus
        0 * _

        where:
        jsonPayload               | expectedStatus
        jsonPayloadInNetwork      | NetworkStatus.InNetwork
        jsonPayloadOutOfNetwork   | NetworkStatus.OutOfNetwork
        jsonPayloadUnknown        | NetworkStatus.Unknown
    }

    void 'test JSON deserialization with invalid network status throws exception'() {
        when:
        mapper.readValue(jsonPayloadInvalid, NetworkStatus)

        then:
        InvalidFormatException ex = thrown(InvalidFormatException)
        ex.message.contains('Cannot deserialize value of type')
        0 * _
    }

    void 'test JSON deserialization with malformed JSON throws exception'() {
        when:
        mapper.readValue('invalid-json', NetworkStatus)

        then:
        JsonParseException ex = thrown(JsonParseException)
        ex.message.contains('Unrecognized token')
        0 * _
    }

    void 'test all enum values are defined'() {
        expect:
        NetworkStatus.values().length == 3
        NetworkStatus.values().contains(NetworkStatus.InNetwork)
        NetworkStatus.values().contains(NetworkStatus.OutOfNetwork)
        NetworkStatus.values().contains(NetworkStatus.Unknown)
    }

    void 'test enum ordering'() {
        expect:
        NetworkStatus.values()[0] == NetworkStatus.InNetwork
        NetworkStatus.values()[1] == NetworkStatus.OutOfNetwork
        NetworkStatus.values()[2] == NetworkStatus.Unknown
    }
}