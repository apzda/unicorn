# Apzda

## 模块说明

| # | 名称           | 说明              |
|:--|:-------------|:----------------|
| 1 | app-common   | 公共类库、公共配置、网关规则  |
| 2 | app-gateway  | 微服务部署时的API网关服务器 |
| 3 | app-server   | 单体应用启动器         |
| 4 | app-services | 应用模块(服务)工程      |
| 5 | apz-services | 内置模块(服务)工程      |

## 中间件依赖

1. MySQL
2. Redis (可选)
3. RocketMQ (可选)

> 启动前需要根据实际情况配置好它们.

## 运行

### 开发环境

1. profile: flyway,gtw,dev
2. JVM 参数: -Djava.net.preferIPv4Stack=true

> 使用`jasypt`加密配置时(默认支持)，添加JVM参数: `-Djasypt.encryptor.password="your-password"`
