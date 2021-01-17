# Socket编程 BIO

创建2个类，测试 socket 连接

SocketIOProperties -服务端

```java
/**
 * BIO 多线程的方式
 */
public class SocketIOProperties {

    // 配置属性的变量
    // ...

    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket();
            server.bind(new InetSocketAddress(9090), BACK_LOG);
            // 设置属性
            // ...
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("server up use 9090");

        try {
            while (true) {
                System.in.read(); // 阻塞线程

                Socket client = server.accept();
                System.out.println("client port: " + client.getPort());

                // 设置属性
                // ...

                new Thread(()-> {
                    try {
                        InputStream in = client.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                        char[] data = new char[1024];

                        while (true) {
                            int num = reader.read(data);

                            if (num > 0) {
                                System.out.println("client read some data is :"
                                        + num + ", value :"
                                        + new String(data, 0 ,num));
                            } else if (num == 0){
                                System.out.println("client readed nothing !");
                                continue;
                            } else {
                                System.out.println("client readed -1...");
                                System.in.read();
                                client.close();
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

```

SocketClient - 客户端

```java
public class SocketClient {

    public static void main(String[] args) {
        try {
            Socket client = new Socket("192.168.163.11", 9090);

            client.setSendBufferSize(20);
            client.setTcpNoDelay(true);
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while (true) {
                String line = reader.readLine();
                if (null != line) {
                    byte[] bytes = line.getBytes();
                    for (byte b : bytes) {
                        out.write(b);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 演示 socket 连接

1、起2个 Linux，把 java 文件 copy进去。起服务器，查看socket 信息与状态

```shell
# 运行 server
javac SocketIOProperties.java && java SocketIOProperties
```

- 开启服务器，线程阻塞

- 查看网络连接

  ```shell
  netstat -natp
      Active Internet connections (servers and established)
      Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name    
      # 监听 9090 端口
      tcp6       0      0 :::9090                 :::*                    LISTEN      1231/java
  ```

- 查看进程信息

  ```shell
  lsof -p 1231
      COMMAND  PID USER   FD   TYPE             DEVICE  SIZE/OFF     NODE NAME
      java    1231 root  cwd    DIR              253,0        94 33596931 /root/testsocket
      java    1231 root    4u  unix 0xffff9980753e9540       0t0    21654 socket
      # 创建 fd 用来监听
      java    1231 root    5u  IPv6              21656       0t0      TCP *:websm (LISTEN)
  ```

- 抓包

  ```shell
  tcpdump -nn -i ens33 port 9090
      tcpdump: verbose output suppressed, use -v or -vv for full protocol decode
      listening on ens33, link-type EN10MB (Ethernet), capture size 262144 bytes
  ```

2、起客户端，查看socket 信息与状态

```shell
# 开启 client
javac SocketClient.java && java SocketClient
```

注意：此时服务器在 System.in.read()，并没有执行到 server.accept()，也就是没有接收客户端数据

- 查看网络连接

  ```shell
  netstat -natp
      Active Internet connections (servers and established)
      Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name             
      tcp6       1      0 :::9090                 :::*                    LISTEN      1231/java    
      # 此时可以发现 server 与 client 已经建立连接，但是没有分配给任何进程使用
      tcp6       0      0 192.168.163.11:9090     192.168.163.12:60270    ESTABLISHED -         
  ```

- 查看进程信息

  ```shell
  lsof -p 1231
      COMMAND  PID USER   FD   TYPE             DEVICE  SIZE/OFF     NODE NAME
      java    1231 root  cwd    DIR              253,0        94 33596931 /root/testsocket
      java    1231 root    4u  unix 0xffff9980753e9540       0t0    21654 socket
      # 还是处于监听状态
      java    1231 root    5u  IPv6              21656       0t0      TCP *:websm (LISTEN)
  ```

- 抓包

  ```shell
  # 命令
  tcpdump -nn -i ens33 port 9090
  
  # 输出
  # 完成三次握手
  06:19:08.459049 IP 192.168.163.12.60270 > 192.168.163.11.9090: Flags [S], seq 1601466075, win 29200, options [mss 1460,sackOK,TS val 2031868 ecr 0,nop,wscale 7], length 0
  06:19:08.459096 IP 192.168.163.11.9090 > 192.168.163.12.60270: Flags [S.], seq 962307413, ack 1601466076, win 1152, options [mss 1460,sackOK,TS val 2035784 ecr 2031868,nop,wscale 0], length 0
  06:19:08.459397 IP 192.168.163.12.60270 > 192.168.163.11.9090: Flags [.], ack 1, win 229, options [nop,nop,TS val 2031868 ecr 2035784], length 0
  
  ```

3、客户端输入并回车，查看socket 信息与状态

输入1234，回车

- 查看网络连接

  ```shell
  netstat -natp
      Active Internet connections (servers and established)
      Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name            
      tcp6       1      0 :::9090                 :::*                    LISTEN      1231/java     
      # 还有没有交由进程处理，但是看到Recv-Q 为4，说明内核已经为连接开辟资源了，就算不传输数据，内核也会分配资源
      tcp6       4      0 192.168.163.11:9090     192.168.163.12:60270    ESTABLISHED -     
  ```

- 查看进程信息

  ```shell
  lsof -p 1231
      COMMAND  PID USER   FD   TYPE             DEVICE  SIZE/OFF     NODE NAME
      java    1231 root  cwd    DIR              253,0        94 33596931 /root/testsocket
      java    1231 root    4u  unix 0xffff9980753e9540       0t0    21654 socket
      # 还是处于监听状态
      java    1231 root    5u  IPv6              21656       0t0      TCP *:websm (LISTEN)
  ```

- 抓包

  ```shell
  # 命令
  tcpdump -nn -i ens33 port 9090
  
  # 输出
  # 可以看出接收到了4个字节
  06:28:27.361476 IP 192.168.163.12.60270 > 192.168.163.11.9090: Flags [P.], seq 1:2, ack 1, win 229, options [nop,nop,TS val 2590771 ecr 2035784], length 1
  06:28:27.361502 IP 192.168.163.11.9090 > 192.168.163.12.60270: Flags [.], ack 2, win 1151, options [nop,nop,TS val 2594687 ecr 2590771], length 0
  06:28:27.361628 IP 192.168.163.12.60270 > 192.168.163.11.9090: Flags [P.], seq 2:5, ack 1, win 229, options [nop,nop,TS val 2590771 ecr 2594687], length 3
  06:28:27.401496 IP 192.168.163.11.9090 > 192.168.163.12.60270: Flags [.], ack 5, win 1148, options [nop,nop,TS val 2594727 ecr 2590771], length 0
  ```

4、服务端回车，程序继续执行，查看socket 信息与状态

注意：此时服务器输入回车执行完成 System.in.read()，执行到 server.accept()，接收客户端数据

- 查看网络连接

  ```shell
  netstat -natp
      Active Internet connections (servers and established)
      Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name            
      tcp6       0      0 :::9090                 :::*                    LISTEN      1231/java    
      # 交由 1231 服务端进程处理
      tcp6       0      0 192.168.163.11:9090     192.168.163.12:60270    ESTABLISHED 1231/java 
  ```

- 查看进程信息

  ```shell
  lsof -p 1231
  
  COMMAND  PID USER   FD   TYPE             DEVICE  SIZE/OFF     NODE NAME
  java    1231 root    4u  unix 0xffff9980753e9540       0t0    21654 socket
  java    1231 root    5u  IPv6              21656       0t0      TCP *:websm (LISTEN)
  # 分配 FD，建立连接
  java    1231 root    6u  IPv6              22649       0t0      TCP usstp75000572.corp.jabil.org:websm->uselm00042331.corp.jabil.org:60270 (ESTABLISHED)
  
  ```

- 抓包，没有变化

### 总结

通过socket 演示，可以得出以下结论

1、server 调用 accept 之前，已经完成三次握手，不管最后是否传输数据，双方内核分别为 socket 开辟资源，也就是内核缓冲区

2、server 调用 accept 之后，双方内核会分配 fd给 tcp，并分配 pid 处理连接

3、accept 方法实际上是创建 fd

### 知识点

**TCP 面向连接的，可靠的传输协议**

1、面向连接

三次握手后，server 与 client 端 内核为 socket 开辟资源，完成面向连接的过程

2、可靠的传输协议

通过 seq、ack 确保 tcp 连接的状态，以及数据的传送

上述测试中，三次握手过程 (服务端，暂时无法获取客户端三次握手监听数据)：

- seq 1601466075
- seq 962307413, ack 1601466076

![tcp 三次握手](images\三次握手.jpg)

**socket**

四元组概念：客户端IP-客户端PORT + 服务器IP-服务器PORT，简化为 CIP_CPORT + SIP_SPORT

四元组也是内核级的，即使不调用 accept，内核也会根据分配的资源以及seq、ack完成数据传输的过程

上述测试中，netstat -natp显示出四元组

```
192.168.163.11:9090  -  192.168.163.12:60270
```

**面试题**

服务端是否需要为客户端连接分配随机端口号？

不需要。四元组可以代表唯一性，在server 和 client都有，不需要再分配端口号

客户端端口号在不关闭的情况下能复用吗？

可以。首先明确的是服务端处于监听状态，端口号不能复用。四元组中，只有客户端port不一样，其他都一样。当 65535 个客户端端口号访问到同一个服务器端口号 X，服务器 80端口不能再使用，但是服务器有其他端口例如 Y，那么客户端可以再使用 65535个端口号连接 Y端口

```
复用客户端端口号
服务端为每个port开启创建进程，进程之间是隔离的，所以客户端port一样，服务器port不同， FD 也可以一样。但是服务端同一个port内 FD不能一样
AIP_CPORT + XIP_XPORT : FD3
AIP_BPORT + XIP_XPORT : FD4
AIP_APORT + XIP_XPORT : FD5

AIP_CPORT + XIP_YPORT : FD3
AIP_BPORT + XIP_YPORT : FD4
AIP_APORT + XIP_YPORT : FD5
```

## 演示 back_log

back_log设置为2，可以先开启3个客户端，然后再开启一个查看状态

1、启动服务器，启动3个客户端

```shell
netstat -natp
    Active Internet connections (servers and established)
    Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name    
    tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      897/sshd            
    tcp        0      0 127.0.0.1:25            0.0.0.0:*               LISTEN      982/master          
    tcp        0      0 192.168.163.11:22       192.168.163.3:59745     ESTABLISHED 1166/sshd: root@not 
    tcp        0     36 192.168.163.11:22       192.168.163.3:59734     ESTABLISHED 985/sshd: root@pts/ 
    tcp        0      0 192.168.163.11:22       192.168.163.3:62604     ESTABLISHED 3787/sshd: root@pts 
    tcp        0      0 192.168.163.11:22       192.168.163.3:59750     ESTABLISHED 1184/sshd: root@pts 
    tcp6       0      0 :::22                   :::*                    LISTEN      897/sshd            
    tcp6       0      0 ::1:25                  :::*                    LISTEN      982/master          
    tcp6       3      0 :::9090                 :::*                    LISTEN      3866/java           
    tcp6       0      0 192.168.163.11:9090     192.168.163.12:60286    ESTABLISHED -                   
    tcp6       0      0 192.168.163.11:9090     192.168.163.12:60288    ESTABLISHED -                   
    tcp6       0      0 192.168.163.11:9090     192.168.163.12:60290    ESTABLISHED -   
```

2、再启动一个客户端

```shell
netstat -natp
    Active Internet connections (servers and established)
    Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name    
    tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN      897/sshd            
    tcp        0      0 127.0.0.1:25            0.0.0.0:*               LISTEN      982/master  
    # 第四个客户端连接不接收，没有第三次握手
    tcp        0      0 192.168.163.11:9090     192.168.163.12:60292    SYN_RECV    -                   
    tcp        0      0 192.168.163.11:22       192.168.163.3:59745     ESTABLISHED 1166/sshd: root@not 
    tcp        0     36 192.168.163.11:22       192.168.163.3:59734     ESTABLISHED 985/sshd: root@pts/ 
    tcp        0      0 192.168.163.11:22       192.168.163.3:62604     ESTABLISHED 3787/sshd: root@pts 
    tcp        0      0 192.168.163.11:22       192.168.163.3:59750     ESTABLISHED 1184/sshd: root@pts 
    tcp6       0      0 :::22                   :::*                    LISTEN      897/sshd            
    tcp6       0      0 ::1:25                  :::*                    LISTEN      982/master          
    tcp6       3      0 :::9090                 :::*                    LISTEN      3866/java           
    tcp6       0      0 192.168.163.11:9090     192.168.163.12:60286    ESTABLISHED -                   
    tcp6       0      0 192.168.163.11:9090     192.168.163.12:60288    ESTABLISHED -                   
    tcp6       0      0 192.168.163.11:9090     192.168.163.12:60290    ESTABLISHED -    
```

back_log就是备用连接可以有多少，再有连接请求就不会接收

