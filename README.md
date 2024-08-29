# Apzda

## 中间件依赖

1. MySQL
2. Redis

> 启动前需要配置它们.

## 内置模块(服务)

1. [配置模块](https://github.com/apzda/config)
2. [验证码模块](https://github.com/apzda/captcha)
3. [存储模块](https://github.com/apzda/oss)
4. [审核日志模块](https://github.com/apzda/audit)
5. [用户中心模块](https://github.com/apzda/ucenter)

## 可选模块(服务)

1. [短信模块](https://github.com/apzda/sms)
2. [信使模块](https://github.com/apzda/messenger)

## 运行

### 开发环境

1. profile: flyway,gtw,dev
2. JVM 参数: -Djava.net.preferIPv4Stack=true

> 使用`jasypt`加密配置时(默认支持)，添加JVM参数: `-Djasypt.encryptor.password="your-password"`
