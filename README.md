persicope=潜望镜
一个本地代理工具
```text

Client <==> Proxy（persicope） <==> Server

               === 
              ||
              ||
         👁 ===
```
实现功能：

[x] 支持http代理协议

[x] 支持https代理协议

[x] 支持Socks4代理协议

[x] 支持Socks5代理协议

程序入口：PeriscopeProxyServer

注意：

谷歌浏览器请配合SwitchyOmega插件进行代理设置

需要https代理协议代理 请在谷歌浏览器加入启动参数：--ignore-certificate-errors

并在UnificationHandler开启SSL处理器


