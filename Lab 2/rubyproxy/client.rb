require 'socket'
require './request'
class Client
  def initialize(host, port)
    # Create a new socket on port.
    # If port is nil, the port is set to 80
    @socket = TCPSocket.new(host, port.nil? ? 80 : port)
    @inappropriate = ['SpongeBob', 'Britney Spears',
                      'Paris Hilton', 'Norrköping',
                      'ParisHilton', 'BritneySpears']
  end

  def send(msg, client_requester)
    # Check if message contains any forbidden words.
    # If so "redirect" to error page.
    if not_allowed(msg.request)

      client_requester.write("HTTP/1.1 302 Moved Permanently\r\n")
      client_requester.write("Location: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error1.html\r\n\r\n")

      client_requester.close
      @socket.close
      false
    else
      @socket.write msg.request
      true
    end
  end

  def receive_data(client)
    content_len = 0
    content_type = ''
    content_enc = ''
    transfer_enc = ''
    header = ''

    # Get the header for incomming message.
    loop do
      line = @socket.readline
      if line =~ /^Content-Length:\s(\d+)/
        content_len = Regexp.last_match(1).to_i
      elsif line =~ /^Content-Type:\s(\w+)/ # OLD /^Content-Type:\s(\w+\/\w+)/
        content_type = Regexp.last_match(1)
      elsif line =~ /^Content-Encoding:\s(\w+)/
        content_enc = Regexp.last_match(1)
      elsif line =~ /^Transfer-Encoding:\s(\w+)/
        transfer_enc = Regexp.last_match(1)
      end
      header << line
      break if line == "\r\n"
    end
    # puts header
    # Change text/html to text
    # && transfer_enc.length == 0
    if content_type == 'text' && content_enc.empty?
      body = @socket.read

      # If the message contains "pure" text, scan for forbidden words.
      # If any is found, "Redirect" to error page and close sockets.
      if not_allowed(body)
        client.write("HTTP/1.1 302 Moved Permanently\r\n")
        client.write("Location: http://www.ida.liu.se/~TDTS04/labs/2011/ass2/error2.html\r\n\r\n")
      else
        msg = header + body
        client.send(msg, msg.size)
      end
    # if not text
    else
      client.write(header)
      body = @socket.read
      client.write(body)
    end

    # All done close all connections
    @socket.close
    client.close
  end

  def not_allowed(req)
    req = req.force_encoding(Encoding::UTF_8)
    @inappropriate.each do |item|
      # RegEX freezes the thread - opted for .include? method.
      if req.downcase.include?(item.downcase)
        puts item
        return true
      end
    end
    false
  end
end
