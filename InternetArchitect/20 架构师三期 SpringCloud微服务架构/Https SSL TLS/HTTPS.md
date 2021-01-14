# Https

![img](images/format,png)

## SSL/TLS

TLS(Transport Layer Security) 是 SSL(Secure Socket Layer) 的后续版本



## 证书生成以及自签名证书

### 查看系统已存证书

certmgr.msc

#### CSR

证书签名请求文件

#### CRT

证书

#### key

私钥

### OPenSSL 自签名

#### 下载

http://slproweb.com/products/Win32OpenSSL.html

#### 证书中的信息

- Country Name (2 letter code) [XX]:CN           #请求签署人的信息
- State or Province Name (full name) []: #请求签署人的省份名字
- Locality Name (eg, city) [Default City]:# 请求签署人的城市名字
- Organization Name (eg, company) [Default Company Ltd]:#请求签署人的公司名字
- Organizational Unit Name (eg, section) []:#请求签署人的部门名字
- Common Name (eg, your name or your server's hostname) []:#这里一般填写请求人的的服务器域名， 



### 服务器端证书

#### 1.生成私钥

找到OpenSSL安装目录下的/bin目录中的OpenSSL.exe

执行命令

`openssl genrsa -des3 -out c:/dev/server.key `

生成私钥，需要提供一个至少4位，最多1023位的密码

#### 2.由私钥创建待签名证书

```
openssl.exe req -new -key c:/dev/server.key -out c:/dev/pub.csr
```

需要依次输入国家，地区，城市，组织，组织单位，Common Name和Email。其中Common Name，可以写自己的名字或者域名，如果要支持https，Common Name应该与域名保持一致，否则会引起浏览器警告。



#### 3.查看证书中的内容

`openssl.exe req -text -in c:/dev/pub.csr -noout`



### 自建CA

我们用的操作系统（windows, linux, unix ,android, ios等）都预置了很多信任的根证书，比如我的windows中就包含VeriSign的根证书，那么浏览器访问服务器比如支付宝www.alipay.com时，SSL协议握手时服务器就会把它的服务器证书发给用户浏览器，而这本服务器证书又是比如VeriSign颁发的，自然就验证通过了。

#### 1.创建CA私钥

`openssl.exe genrsa -out c:/dev/myca.key 2048`

#### 2.生成CA待签名证书

`openssl.exe req -new -key c:/dev/myca.key -out c:/dev/myca.csr`

#### 3.生成CA根证书

`openssl.exe x509 -req -in c:/dev/myca.csr -extensions v3_ca -signkey c:/dev/myca.key -out myca.crt`

#### 4.对服务器证书签名

`openssl x509 -days 365 -req -in c:/dev/pub.csr -extensions v3_req -CAkey c:/dev/myca.key -CA c:/dev/myca.crt -CAcreateserial -out c:/dev/server.crt`



### Nginx配置

```
     server {
             listen       443 ssl;
             server_name  aa.abc.com;

             ssl_certificate      /data/cert/server.crt;
             ssl_certificate_key  /data/cert/server.key;

     }
```

### 信任

 在系统中安装证书



### 图形化工具

https://sourceforge.net/projects/xca/



## 线上服务器安装配置

gigsgigscloud.com



cn2 gia

### 域名解析

## 免费签名

https://freessl.cn

## SS安装

服务器端

```

wget --no-check-certificate https://raw.githubusercontent.com/teddysun/shadowsocks_install/master/shadowsocksR.sh
chmod +x shadowsocksR.sh
./shadowsocksR.sh 2>&1 | tee shadowsocksR.log
```

卸载

```
./shadowsocksR.sh uninstall
```

运行状态

```
/etc/init.d/shadowsocks status
```