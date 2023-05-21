vcl 4.1;

import std;

backend default {
    .host = "hornsup";
    .port = "8443";
}

sub vcl_recv {
  if (req.method != "GET" && req.method != "HEAD") {
    return (pass);
  }

    # purge the entire cache on every POST
    # if ( req.request == "POST") {
    #     ban("req.http.host == " + req.http.Host);
    #     return(pass);
    # }

    # Happens before we check if we have this in cache already.
  # if ((client.ip != "127.0.0.1" && std.port(server.ip) == 80) &&
  #     (req.http.host ~ "hornsup")) {
  #   set req.http.x-redir = "https://" + req.http.host + req.url;
  #   return (synth(750, ""));
  # }
}

#sub vcl_backend_response {
    # Happens after we have read the response headers from the backend.
    #
    # Here you clean the response headers, removing silly Set-Cookie headers
    # and other mistakes your backend does.
#}

sub vcl_deliver {
  if (obj.hits > 0) {
    set resp.http.X-Cache-Action = "HIT";
    set resp.http.X-Cache-Hits = obj.hits;
  } else {
    set resp.http.X-Cache-Action = "MISS";
  }

  set resp.http.X-Powered-By = "SpringBoot";

  if (req.method == "OPTIONS") {
    set resp.http.X-Origin = req.http.origin;
#    set resp.http.Access-Control-Allow-Origin = req.http.origin;
#    set resp.http.Access-Control-Allow-Methods = "GET, POST, OPTIONS";
#    set resp.http.Access-Control-Allow-Headers = "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
  }

  unset resp.http.Server;
}

sub vcl_synth {
  # Listen to 750 status from vcl_recv.
  # if (resp.status == 750) {
  #   // Redirect to HTTPS with 301 status.
  #   set resp.status = 301;
  #   set resp.http.Location = req.http.x-redir;
  #   return(deliver);
  # }
}

# sub vcl_recv {
#     if (std.port(server.ip) != 443) {
#         set req.http.location = "https://" + req.http.host + req.url;
#         return(synth(301));
#     }
# }
#

sub vcl_backend_response {
  # Removing Set-Cookie from static responses regardless of query string.
  if (bereq.url ~ "(?i)\.(?:css|gif|ico|jpeg|jpg|js|json|png|swf|woff)(?:\?.*)?$") {
    unset beresp.http.set-cookie;
  }

  if (bereq.method == "OPTIONS") {
    # set beresp.http.Access-Control-Allow-Origin = req.http.My-Origin;
    set beresp.http.Access-Control-Allow-Origin = bereq.http.origin;
    set beresp.http.Access-Control-Allow-Methods = "GET, POST, OPTIONS";
    set beresp.http.Access-Control-Allow-Headers = "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range";
    # set beresp.ttl = 1h;  // Set an appropriate cache TTL for OPTIONS requests
    return (deliver);
  }
}

# vim: set ft=conf:
