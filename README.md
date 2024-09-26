# Apzda Scaffold

[![Integration Tests](https://github.com/apzda/apzda/actions/workflows/integration.yml/badge.svg)](https://github.com/apzda/apzda/actions/workflows/integration.yml)

## 模块说明

| # | 名称           | 说明              |
|:--|:-------------|:----------------|
| 1 | app-common   | 公共类库、公共配置、网关规则  |
| 2 | app-gateway  | 微服务部署时的API网关服务器 |
| 3 | app-server   | 单体应用启动器         |
| 4 | app-services | 应用模块(服务)工程      |
| 5 | apz-services | 内置模块(服务)工程      |

## 通用基础服务

| # | 名称        | 服务ID | 说明                                                              |
|:--|:----------|:-----|:----------------------------------------------------------------|
| 1 | config    | 900  | 应用设置(首选项)服务                                                     |
| 2 | sms       | 901  | 短信服务, 通道支持: 阿里大鱼、创蓝。可自由扩展                                       |
| 3 | uc        | 902  | 用户中心                                                            |
| 4 | wallet    | 903  | 用户钱包, 可以自定义币种与汇率，支持有效期机制                                        |                                                      
| 5 | audit     | 904  | 审计日志服务                                                          |
| 6 | captcha   | 905  | 验证码服务，支持: 图片验证码、滑动验证码、拖动验证码。可自由扩展                               |
| 7 | oss       | 906  | 对象存储服务, 后端已支持: 阿里云OSS、腾讯云COS、MinIO和本地存储。内置插件: 添加水印和Resize。可自由扩展 |
| 8 | messenger | 908  | 信使服务, 基于事务消息实现，保证信件投递结果与数据库结果一致性。可自由扩展                          |

## 中间件依赖

1. MySQL
2. Redis (可选)
3. RocketMQ (可选)

> 启动前需要根据实际情况配置好它们.

## 编译

因使用Protocol Buffers定义接口，在启动前需要执行`mvn compile`进行编译。

> 每此改动接口定义文件都需要重新编译！！！
>

## 运行

1. profile: flyway,gtw,dev
2. JVM 参数: -Djava.net.preferIPv4Stack=true

> 使用`jasypt`加密配置时(默认支持)，添加JVM参数: `-Djasypt.encryptor.password="your-password"`

## 加密配置

使用`com.github.ulisesbocchio:jasypt-maven-plugin`插件可以对配置项的值进行加/解密:

1. 加密: `mvn jasypt:encrypt-value -Djasypt.encryptor.password="your-password" -Djasypt.plugin.value="value"`
2. 解密: `mvn jasypt:decrypt-value -Djasypt.encryptor.password="your-password" -Djasypt.plugin.value="ENC(xxyy)"`

> 更多参数:
>
> 1. `-Djasypt.plugin.decrypt.prefix`: The decrypted property prefix, Default: `DEC(`
> 2. `-Djasypt.plugin.decrypt.suffix`: The decrypted property suffix, Default: `)`
> 3. `-Djasypt.plugin.encrypt.prefix`: The encrypted property prefix, Default: `ENC(`
> 4. `-Djasypt.plugin.encrypt.suffix`: The encrypted property suffix, Default: `)`
>
> > 特别说明: `PowerShell`中`-Djasypt.encryptor.password=xxxxx`要改为`-D"jasypt.encryptor.password=xxxxx"`
