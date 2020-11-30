persicope=潜望镜

```text

Client <==> Proxy（persicope） <==> Server

               === 
              ||
              ||
         👁 ===
```

一个本地代理抓包工具


程序入口：PeriscopeProxyServer

需要https代理请在浏览器加入启动参数：--ignore-certificate-errors

谷歌浏览器请配合SwitchyOmega插件进行设置