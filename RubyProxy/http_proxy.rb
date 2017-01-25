require 'socket'
require 'net/http'

class HttpProxy

  # @param [Integer] port
  def run(port=9999)
    begin
      begin
        @socket = TCPServer.new(port)
        puts "Starting server at port #{port}"
      rescue
        puts "Couldn't open a new Proxy server at the port #{port}!"
        exit(1)
      end

      loop do
        client_connection = @socket.accept
        puts 'Client connected...'
        puts 'Redirecting to a new thread...'
        Thread.new(client_connection, &method(:HandleRequest))
      end

    rescue StandardError
      puts 'Something went wrong...'
    rescue Interrupt
      puts ''
      puts 'User shutting down Proxy Server...'
    ensure
      if @socket
        @socket.close
        puts 'Closing down the socket just in case...'
        puts 'Qutting the Proxy Server'
        exit(1)
      end
    end
  end


  def HandleRequest(client)
    # What does client want to do?
    # EXAMPLE: GET http://www.w3.org/pub/WWW/TheProject.html HTTP/1.1
    request_line = client.readline
    puts request_line
    # Extract all keywords
    # REGEX: (^\w+)\s+(\S*)\s+(HTTP\/1.\d)$

    # doesn't work? print good but memory not good
    #verb, url ,version = request_line.scan(/(^\w+)\s+(\S*)\s+(HTTP\/1.\d)/)

    verb = request_line[/^\w+/]
    url = request_line[/^\w+\s+(\S+)/, 1]
    version = request_line[/^\w+\s+\S+\s(HTTP\/1.\d)/, 1]


    #uri = URI.parse("http://www.google.se/")
    uri = URI.parse(url)
    p uri.host
    p uri.path
    p version

    #modify?

    # Open connection to target server
    target_server = TCPSocket.new(uri.host, uri.port)
    # Send request to target server
    puts "#{verb} #{uri.path} #{version}\r\n"
    target_server.write("#{verb} #{uri.path} #{version}")
    #close the connection to target_server (maybe keep-alive to keep feeding data to client?)
    target_server.write("Connection: close\r\n\r\n")


    #while (buffer = target_server.recv(2048))
    #  p buffer
    #end
    buffer = ''
    while (tmp = target_server.gets)
      buffer += tmp
    end
    header, body = buffer.split("\r\n\r\n", 2)
    puts body
    puts header
    # Loop to get all data


    # Check if forbidden
    #if HasForbidden(buffer) #TODO: Check for version 1.0!!!!!!
      #client.write("HTTP/1.1 301 Moved Permanently\r\nLocation: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html\r\n\r\n")
    #else
      puts "YEA"
      client.write(buffer)
    #end

    # Close connections and sockets
    client.close

    if target_server
      target_server.close
    end

  end

  #HasForbidden Method
  # Use Regex to see if forbidden words exists in given buffer
  # Return True if has forbidden words
  # Return False
  #End HasForbidden

  def HasForbidden(buffer)
    forbidden_words = ["SpongeBob, Britney Spears, Paris Hilton, or Norrköping"]
    forbidden_words.each do |word|
      if buffer =~ /#{word}/
        return true
      end
    end
    return false
  end

end

HttpProxy.new.run()


