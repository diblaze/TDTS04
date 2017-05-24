require 'socket'
require './server'
require './client'
require './request'
class Proxy
  def initialize(port)
    @server = Server_socket.new(port)
  end

  def start
    # Start server, if there are errors sockets will be closed

    # Create server socket
    @server.create
    loop do
      # Wait for incomming connection
      new_client = @server.accept
      # handle_req(new_client)
      # Handle request in a new thread
      Thread.new(new_client, &method(:handle_req))
    end
  # Error handling if user press CTRL-C
  rescue Interrupt
    puts 'Exit...'
  # Make sure that the sockets are closed
  ensure
    if @server
      @server.close
      puts 'Sockets closed...'
    end
    puts 'Bye.'
  end

  def handle_req(client_request)
    # Make a request obj.
    req = Request.new(client_request)

    # If request is not a GET request, just leave...
    return if req.type != 'GET'
    # Create a socket for the new request and send it.
    # and receive the response from server
    client = Client.new(req.host, req.port)
    return unless client.send(req, client_request)

    client.receive_data(client_request)
  end
end

# Get parameters and start the server
# If no args is given, port 2000 is used
if ARGV.empty?
  port = 2000
elsif ARGV.size == 1
  port = ARGV[0].to_i
end

proxyserver = Proxy.new(port)
proxyserver.start
