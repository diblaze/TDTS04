require 'socket'
class Request
  attr_reader :type, :port, :host, :request
  def initialize(request)
    @request = ''
    close_exist = false

    request.each do |i|
      i.sub!(/http:\/\/[\w.-]+\//, '/') if i =~ /^GET/
      if i =~ /Connection:/
        @request << "Connection: close\r\n"
        close_exist = true
      elsif i =~ /Accept-Encoding:/
        @request << "Accept-Encoding: 'gzip;q=0, identity, *'\r\n"
      else
        @request << i
      end
      break if i == "\r\n"
    end
    @request << "Connection: close\r\n" unless close_exist
    @port = @request[/Host:\s[\w.]+:(\d+)/, 1]
    @type = @request[/^\w+/]
    @host = @request[/Host:\s([\w\.\-]+)/, 1]
    @host = @request[/\/\/([\w\.\-]+)/, 1] if @host.nil?
    # puts @request
  end
end
