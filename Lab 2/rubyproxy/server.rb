require 'socket'
class Server_socket
  def initialize(port)
    @port = port
    @host_name = '127.0.0.1' # or localhost
    @socket = nil
  end

  def create
    # bind to port and listen
    # TCPServer makes a bind and listen
    @socket = TCPServer.new(@host_name, @port)
  end

  def accept
    # accept incomming connections
    @socket.accept
  end

  def close
    # Close socket
    @socket.close
  end
end
