vcl 4.1;

import std;

# Default backend definition. Set this to point to your content server.
backend default {
    .host = "hornsup";
    .port = "8443";
}

sub vcl_recv {
    # Happens before we check if we have this in cache already.
  if ((client.ip != "127.0.0.1" && std.port(server.ip) == 80) &&
      (req.http.host ~ "hornsup")) {
    set req.http.x-redir = "https://" + req.http.host + req.url;
    return (synth(750, ""));
  }
}

sub vcl_backend_response {
    # Happens after we have read the response headers from the backend.
    #
    # Here you clean the response headers, removing silly Set-Cookie headers
    # and other mistakes your backend does.
}

sub vcl_deliver {
    # Happens when we have all the pieces we need, and are about to send the
    # response to the client.
    #
    # You can do accounting or modifying the final object here.
}

sub vcl_synth {
  # Listen to 750 status from vcl_recv.
  if (resp.status == 750) {
    // Redirect to HTTPS with 301 status.
    set resp.status = 301;
    set resp.http.Location = req.http.x-redir;
    return(deliver);
  }
}

# sub vcl_recv {
#     if (std.port(server.ip) != 443) {
#         set req.http.location = "https://" + req.http.host + req.url;
#         return(synth(301));
#     }
# }
