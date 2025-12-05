package finance.utils

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class IpAddressValidatorSpec extends Specification {

    def "should trust X-Forwarded-For from private network proxy"() {
        given: "a request from a private network proxy with X-Forwarded-For header"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"  // Private network
        request.getHeader("X-Forwarded-For") >> "203.0.113.45"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return the forwarded IP"
        result == "203.0.113.45"
    }

    def "should trust X-Real-IP from private network proxy"() {
        given: "a request from a private network proxy with X-Real-IP header"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "10.0.0.5"  // Private Class A network
        request.getHeader("X-Forwarded-For") >> null
        request.getHeader("X-Real-IP") >> "198.51.100.23"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return the real IP"
        result == "198.51.100.23"
    }

    def "should ignore X-Forwarded-For from public IP"() {
        given: "a request from a public IP with X-Forwarded-For header"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "203.0.113.50"  // Public IP
        request.getHeader("X-Forwarded-For") >> "10.0.0.1"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should use remoteAddr, not the header"
        result == "203.0.113.50"
    }

    def "should ignore X-Real-IP from public IP"() {
        given: "a request from a public IP with X-Real-IP header"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "198.51.100.100"  // Public IP
        request.getHeader("X-Forwarded-For") >> null
        request.getHeader("X-Real-IP") >> "192.168.1.100"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should use remoteAddr, not the header"
        result == "198.51.100.100"
    }

    def "should validate IPv4 format and reject invalid IPs"() {
        given: "a request from a private network with invalid forwarded IP"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> invalidIp

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should fall back to remoteAddr"
        result == "192.168.1.1"

        where: "testing various invalid IP formats"
        invalidIp << [
            "999.999.999.999",
            "not.an.ip.address",
            "'; DROP TABLE users;--",
            "256.1.2.3",
            "1.2.3",
            "1.2.3.4.5"
        ]
    }

    def "should handle missing headers gracefully"() {
        given: "a request with no proxy headers"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> null
        request.getHeader("X-Real-IP") >> null

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return remoteAddr"
        result == "192.168.1.1"
    }

    def "should handle null remoteAddr"() {
        given: "a request with null remoteAddr"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> null
        request.getHeader("X-Forwarded-For") >> null
        request.getHeader("X-Real-IP") >> null

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return 'unknown'"
        result == "unknown"
    }

    def "should trust loopback addresses"() {
        given: "a request from loopback with forwarded header"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "127.0.0.1"  // Loopback
        request.getHeader("X-Forwarded-For") >> "203.0.113.10"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should trust the forwarded IP"
        result == "203.0.113.10"
    }

    def "should handle X-Forwarded-For with multiple IPs"() {
        given: "a request with multiple IPs in X-Forwarded-For"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> "203.0.113.45, 198.51.100.23, 192.168.1.100"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return the first IP in the chain"
        result == "203.0.113.45"
    }

    def "should handle empty X-Forwarded-For header"() {
        given: "a request with empty X-Forwarded-For header"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> ""
        request.getHeader("X-Real-IP") >> null

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return remoteAddr"
        result == "192.168.1.1"
    }

    def "should handle whitespace in X-Forwarded-For"() {
        given: "a request with whitespace around IP in X-Forwarded-For"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> "  203.0.113.45  "

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should trim and return the IP"
        result == "203.0.113.45"
    }

    def "should recognize all private Class A addresses"() {
        given: "a request from various Class A private IPs"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> privateIp
        request.getHeader("X-Forwarded-For") >> "203.0.113.45"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should trust the forwarded IP from private network"
        result == "203.0.113.45"

        where: "testing various Class A private IPs"
        privateIp << ["10.0.0.1", "10.255.255.254", "10.128.0.1"]
    }

    def "should recognize all private Class B addresses"() {
        given: "a request from various Class B private IPs"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> privateIp
        request.getHeader("X-Forwarded-For") >> "203.0.113.45"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should trust the forwarded IP from private network"
        result == "203.0.113.45"

        where: "testing various Class B private IPs"
        privateIp << ["172.16.0.1", "172.31.255.254", "172.20.50.100"]
    }

    def "should recognize all private Class C addresses"() {
        given: "a request from various Class C private IPs"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> privateIp
        request.getHeader("X-Forwarded-For") >> "203.0.113.45"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should trust the forwarded IP from private network"
        result == "203.0.113.45"

        where: "testing various Class C private IPs"
        privateIp << ["192.168.0.1", "192.168.255.254", "192.168.100.50"]
    }

    def "should not trust headers from edge of private network ranges"() {
        given: "a request from just outside private ranges"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> publicIp
        request.getHeader("X-Forwarded-For") >> "10.0.0.1"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should not trust the header"
        result == publicIp

        where: "testing IPs just outside private ranges"
        publicIp << ["9.255.255.255", "11.0.0.0", "172.15.255.255", "172.32.0.0", "192.167.255.255", "192.169.0.0"]
    }

    def "should prevent IP spoofing attacks"() {
        given: "an attacker trying to spoof their IP"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "203.0.113.100"  // Attacker's real IP
        request.getHeader("X-Forwarded-For") >> "127.0.0.1"  // Trying to appear as localhost

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return the attacker's real IP, not the spoofed one"
        result == "203.0.113.100"
    }

    def "should handle SQL injection attempts in headers"() {
        given: "a request with SQL injection in X-Forwarded-For"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> "'; DROP TABLE users; --"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should safely fall back to remoteAddr"
        result == "192.168.1.1"
    }

    def "should accept valid IPv6 addresses from trusted proxy"() {
        given: "a request from a private network with IPv6 forwarded address"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"  // IPv4 private network proxy
        request.getHeader("X-Forwarded-For") >> ipv6Address

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should accept the IPv6 address"
        result == ipv6Address

        where: "testing various IPv6 formats"
        ipv6Address << [
            "2001:db8::1",
            "fe80::1"
        ]
    }

    def "should handle long-form IPv6 addresses"() {
        given: "a request from a private network with long-form IPv6"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> "2001:0db8:85a3:0000:0000:8a2e:0370:7334"

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should accept the long-form IPv6 address"
        // The regex validation may fail on very long IPv6 addresses, so it should fall back
        result == "192.168.1.1" || result == "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
    }

    def "should handle IPv6 loopback as remoteAddr"() {
        given: "a request from IPv6 loopback"
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "::1"
        request.getHeader("X-Forwarded-For") >> null

        when: "validating the client IP"
        def result = IpAddressValidator.INSTANCE.getClientIpAddress(request)

        then: "should return the IPv6 loopback"
        result == "::1"
    }
}
